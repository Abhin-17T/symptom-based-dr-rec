package edu.project.medisuggest;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate db; private final BCryptPasswordEncoder encoder;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    public ApiController(JdbcTemplate db, BCryptPasswordEncoder encoder) { this.db=db; this.encoder=encoder; }

    private Map<String,Object> error(String message) { return Map.of("error", message); }
    private Map<String,Object> user(HttpSession s) { Object id=s.getAttribute("userId"); if(id==null)return null;
        List<Map<String,Object>> rows=db.queryForList("SELECT id,full_name AS name,email,phone,role FROM users WHERE id=?",id);
        return rows.isEmpty()?null:rows.get(0); }
    private Map<String,Object> requireUser(HttpSession s) { Map<String,Object> u=user(s); if(u==null)throw new ApiError(HttpStatus.UNAUTHORIZED,"Please sign in to continue."); return u; }
    private Map<String,Object> requireAdmin(HttpSession s) { Map<String,Object> u=requireUser(s); if(!"ADMIN".equals(u.get("role")))throw new ApiError(HttpStatus.FORBIDDEN,"Administrator access required."); return u; }

    @GetMapping("/auth/me") public Map<String,Object> me(HttpSession s) { Map<String,Object> u=user(s); return Map.of("authenticated",u!=null,"user",u==null?Map.of():u); }
    @PostMapping("/auth/register") public ResponseEntity<?> register(@RequestBody RegisterRequest r,HttpSession s) {
        String name=clean(r.name), email=clean(r.email).toLowerCase(), password=r.password==null?"":r.password;
        if(name.length()<2||name.length()>120)return ResponseEntity.badRequest().body(error("Enter your full name (2–120 characters)."));
        if(!EMAIL.matcher(email).matches())return ResponseEntity.badRequest().body(error("Enter a valid email address."));
        if(password.length()<8)return ResponseEntity.badRequest().body(error("Password must contain at least 8 characters."));
        if(db.queryForObject("SELECT COUNT(*) FROM users WHERE email=?",Integer.class,email)>0)return ResponseEntity.status(409).body(error("An account with this email already exists."));
        db.update("INSERT INTO users(full_name,email,phone,password_hash,role) VALUES(?,?,?,?, 'PATIENT')",name,email,clean(r.phone),encoder.encode(password));
        Long id=db.queryForObject("SELECT id FROM users WHERE email=?",Long.class,email); s.setAttribute("userId",id);
        return ResponseEntity.ok(Map.of("message","Account created","user",user(s)));
    }
    @PostMapping("/auth/login") public ResponseEntity<?> login(@RequestBody LoginRequest r,HttpSession s) {
        String email=clean(r.email).toLowerCase(); List<Map<String,Object>> rows=db.queryForList("SELECT id,password_hash FROM users WHERE email=?",email);
        if(rows.isEmpty()||r.password==null||!encoder.matches(r.password,(String)rows.get(0).get("password_hash")))return ResponseEntity.status(401).body(error("Email or password is incorrect."));
         s.setAttribute("userId",rows.get(0).get("id")); return ResponseEntity.ok(Map.of("message","Signed in","user",user(s)));
    }
    @PostMapping("/auth/logout") public Map<String,String> logout(HttpSession s) { s.invalidate(); return Map.of("message","Signed out"); }

    @GetMapping("/doctors") public List<Map<String,Object>> doctors(@RequestParam(required=false) String specialty) {
        if(specialty!=null&&!specialty.isBlank())return db.queryForList("SELECT id,full_name AS name,specialty,experience_years AS experience,rating,bio,available_days AS availableDays,available FROM doctors WHERE available=TRUE AND specialty=? ORDER BY rating DESC",specialty);
        return db.queryForList("SELECT id,full_name AS name,specialty,experience_years AS experience,rating,bio,available_days AS availableDays,available FROM doctors ORDER BY available DESC,rating DESC");
    }
    @PostMapping("/recommend") public Map<String,Object> recommend(@RequestBody SymptomRequest r) {
        List<String> symptoms=r.symptoms==null?List.of():r.symptoms.stream().map(x->x.toLowerCase(Locale.ROOT)).toList();
        if(symptoms.isEmpty()&&(r.details==null||r.details.isBlank()))throw new ApiError(HttpStatus.BAD_REQUEST,"Select at least one symptom or describe what you are feeling.");
        String all=String.join(" ",symptoms)+" "+(r.details==null?"":r.details.toLowerCase(Locale.ROOT));
        boolean emergency=containsAny(all,"chest pain","difficulty breathing","can't breathe","cannot breathe","shortness of breath","fainting","unconscious","severe bleeding","stroke","face drooping","suicidal");
        String specialty="General Physician", reason="A general physician is a good first point of contact for these symptoms.";
        if(containsAny(all,"rash","itch","skin","acne","eczema")){specialty="Dermatologist";reason="A dermatologist can assess skin-related symptoms.";}
        else if(containsAny(all,"headache","migraine","dizziness","numbness","seizure")){specialty="Neurologist";reason="A neurologist may be appropriate for headache or neurological symptoms.";}
        else if(containsAny(all,"heart","palpitation","blood pressure","chest")){specialty="Cardiologist";reason="A cardiologist may assess heart or blood-pressure concerns.";}
        else if(containsAny(all,"joint","bone","back pain","knee","sprain","muscle")){specialty="Orthopedist";reason="An orthopedist may help with bone, joint or muscle concerns.";}
        else if(containsAny(all,"child","baby","infant","pediatric")){specialty="Pediatrician";reason="For a child, a pediatrician is an appropriate first contact.";}
        String advice="Rest, drink fluids if you can, and monitor how you feel. For any over-the-counter product, follow its label and check with a pharmacist or clinician first—especially for children, pregnancy, allergies, or existing conditions. No medicine is prescribed by this tool.";
        if(containsAny(all,"fever","cough","cold","sore throat","fatigue"))advice="Rest and drink fluids if you can. For fever or discomfort, ask a pharmacist or clinician whether an over-the-counter option is safe for you; follow the product label and do not combine products with the same active ingredient. Seek care if symptoms worsen or persist.";
        if(containsAny(all,"stomach","nausea","vomit","diarrhea"))advice="Take small frequent sips of water or oral rehydration solution if appropriate. Avoid self-starting medicines—some are unsuitable depending on cause, age, pregnancy, or other conditions. Seek clinical advice if symptoms persist or worsen.";
        return Map.of("specialty",specialty,"reason",reason,"temporaryGuidance",advice,"urgent",emergency,
                "urgentMessage",emergency?"This may be an emergency. Call your local emergency number or go to the nearest emergency department now; do not wait for an online appointment.":"This is general information, not a diagnosis or a substitute for medical care.","disclaimer","Symptom matching is educational and can be wrong. Only a qualified clinician can diagnose or recommend treatment.");
    }
    @PostMapping("/appointments") public ResponseEntity<?> book(@RequestBody AppointmentRequest r,HttpSession s) {
        Map<String,Object> u=requireUser(s);
        try {
            LocalDate date=LocalDate.parse(r.date); LocalTime time=LocalTime.parse(r.time);
            if(date.isBefore(LocalDate.now())||date.isAfter(LocalDate.now().plusMonths(3)))return ResponseEntity.badRequest().body(error("Choose a date within the next 3 months."));
            if(time.isBefore(LocalTime.of(9,0))||time.isAfter(LocalTime.of(16,30))||time.getMinute()%30!=0)return ResponseEntity.badRequest().body(error("Choose a 30-minute slot between 09:00 and 16:30."));
            Integer dc=db.queryForObject("SELECT COUNT(*) FROM doctors WHERE id=? AND available=TRUE",Integer.class,r.doctorId);
            if(dc==null||dc==0)return ResponseEntity.badRequest().body(error("This doctor is not currently accepting appointments."));
            String day=date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT,Locale.ENGLISH);
            String days=db.queryForObject("SELECT available_days FROM doctors WHERE id=?",String.class,r.doctorId);
            if(!Arrays.asList(days.split(",")).contains(day))return ResponseEntity.badRequest().body(error("This doctor is not available on that day. Select another date."));
            db.update("INSERT INTO appointments(user_id,doctor_id,appointment_date,appointment_time,reason,status) VALUES(?,?,?,?,?,'PENDING')",u.get("id"),r.doctorId,date,time,trimTo(r.reason,500));
            return ResponseEntity.ok(Map.of("message","Appointment request submitted","appointment",db.queryForMap("SELECT a.id,a.appointment_date AS date,a.appointment_time AS time,a.status,d.full_name AS doctor,d.specialty FROM appointments a JOIN doctors d ON d.id=a.doctor_id WHERE a.user_id=? ORDER BY a.id DESC LIMIT 1",u.get("id"))));
        } catch(DateTimeException e) {return ResponseEntity.badRequest().body(error("Choose a valid date and time."));}
          catch(org.springframework.dao.DuplicateKeyException e) {return ResponseEntity.status(409).body(error("That time slot has just been booked. Please choose another."));}
    }
    @GetMapping("/appointments/mine") public List<Map<String,Object>> mine(HttpSession s) { Map<String,Object> u=requireUser(s); return db.queryForList("SELECT a.id,a.appointment_date AS date,a.appointment_time AS time,a.reason,a.status,d.full_name AS doctor,d.specialty FROM appointments a JOIN doctors d ON d.id=a.doctor_id WHERE a.user_id=? ORDER BY a.appointment_date DESC,a.appointment_time",u.get("id")); }
    @DeleteMapping("/appointments/{id}") public ResponseEntity<?> cancel(@PathVariable long id,HttpSession s) {Map<String,Object> u=requireUser(s); int n=db.update("UPDATE appointments SET status='CANCELLED' WHERE id=? AND user_id=? AND status IN ('PENDING','CONFIRMED')",id,u.get("id")); return n==0?ResponseEntity.badRequest().body(error("Appointment could not be cancelled.")):ResponseEntity.ok(Map.of("message","Appointment cancelled"));}

    @GetMapping("/admin/appointments") public List<Map<String,Object>> allAppointments(HttpSession s) {requireAdmin(s);return db.queryForList("SELECT a.id,u.full_name AS patient,u.email,d.full_name AS doctor,d.specialty,a.appointment_date AS date,a.appointment_time AS time,a.reason,a.status FROM appointments a JOIN users u ON u.id=a.user_id JOIN doctors d ON d.id=a.doctor_id ORDER BY a.appointment_date DESC,a.appointment_time");}
    @PatchMapping("/admin/appointments/{id}") public ResponseEntity<?> updateAppointment(@PathVariable long id,@RequestBody StatusRequest r,HttpSession s) {requireAdmin(s); if(!Set.of("CONFIRMED","CANCELLED","COMPLETED").contains(r.status))return ResponseEntity.badRequest().body(error("Invalid appointment status.")); int n=db.update("UPDATE appointments SET status=? WHERE id=?",r.status,id);return n==0?ResponseEntity.notFound().build():ResponseEntity.ok(Map.of("message","Appointment updated"));}
    @PatchMapping("/admin/doctors/{id}") public ResponseEntity<?> updateDoctor(@PathVariable long id,@RequestBody DoctorUpdate r,HttpSession s) {requireAdmin(s);if(r.available==null)return ResponseEntity.badRequest().body(error("Availability value is required."));int n=db.update("UPDATE doctors SET available=? WHERE id=?",r.available,id);return n==0?ResponseEntity.notFound().build():ResponseEntity.ok(Map.of("message","Doctor availability updated"));}
    @GetMapping("/admin/stats") public Map<String,Object> stats(HttpSession s) {requireAdmin(s);return Map.of("patients",db.queryForObject("SELECT COUNT(*) FROM users WHERE role='PATIENT'",Integer.class),"doctors",db.queryForObject("SELECT COUNT(*) FROM doctors",Integer.class),"appointments",db.queryForObject("SELECT COUNT(*) FROM appointments",Integer.class),"pending",db.queryForObject("SELECT COUNT(*) FROM appointments WHERE status='PENDING'",Integer.class));}

    private static String clean(String v){return v==null?"":v.trim();} private static String trimTo(String v,int n){String x=clean(v);return x.length()>n?x.substring(0,n):x;} private static boolean containsAny(String s,String... terms){for(String t:terms)if(s.contains(t))return true;return false;}
    public record RegisterRequest(String name,String email,String phone,String password){} public record LoginRequest(String email,String password){}
    public record SymptomRequest(List<String> symptoms,String details){} public record AppointmentRequest(Long doctorId,String date,String time,String reason){}
    public record StatusRequest(String status){} public record DoctorUpdate(Boolean available){}
    @RestControllerAdvice static class Errors { @ExceptionHandler(ApiError.class) ResponseEntity<?> api(ApiError e){return ResponseEntity.status(e.status).body(Map.of("error",e.getMessage()));} }
    static class ApiError extends RuntimeException { final HttpStatus status; ApiError(HttpStatus s,String m){super(m);status=s;} }
}
