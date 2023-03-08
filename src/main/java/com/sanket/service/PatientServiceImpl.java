package com.sanket.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sanket.entity.Appointment;
import com.sanket.entity.CurrentPatientSession;
import com.sanket.entity.Doctor;
import com.sanket.entity.Patient;
import com.sanket.exception.AppointmentException;
import com.sanket.exception.DoctorException;
import com.sanket.exception.LoginException;
import com.sanket.exception.PatientException;
import com.sanket.exception.TimeDateException;
import com.sanket.repository.SessionDao;
import com.sanket.repository.AppointmentDao;
import com.sanket.repository.DoctorDao;
import com.sanket.repository.PatientDao;

@Service
public class PatientServiceImpl implements PatientService {
	
	public static Map<String, LocalDateTime> myTimeDate = new LinkedHashMap<>();
	
	@Autowired
	PatientDao userDao;
	
	@Autowired
	SessionDao sessionDao;
	
	@Autowired
	PatientDao patientDao;
	
	@Autowired
	AppointmentDao appointmentDao;
	
	@Autowired
	DoctorDao doctorDao;

	@Override
	public Patient createPatient(Patient patient) throws PatientException {
		
		Patient databaseUser = userDao.findByMobileNo(patient.getMobileNo()); 
		
		if(databaseUser == null) { 
			
			userDao.save(patient);
			
			return patient; 
			
		
		}else {	
			
			throw new PatientException("Patient already register with this mobile no " + patient.getMobileNo());
			
		}
	}

	@Override
	public Patient updatePatient(Patient user, String key) throws PatientException {
		

		CurrentPatientSession loggedInUser = sessionDao.findByUuid(key);
		
		if(loggedInUser == null) {
			
			throw new PatientException("Please provide the valid key to update the user");
		}
		
		if(user.getPatientId() == loggedInUser.getUserId()) {
			
			return userDao.save(user);
	
		}else {
			throw new PatientException("Invalid user details. Login first");
		}
	}

	@Override
	public Patient getPatientByUuid(String uuid) throws PatientException {
		
		CurrentPatientSession currentPatient = sessionDao.findByUuid(uuid);
		
		Optional<Patient> patient = userDao.findById(currentPatient.getUserId());
		
		if(patient.isPresent()) {
			
			return patient.get();
		
		}else {
			
			throw new PatientException("Customer not present by this uuid " + uuid);
		}
	}

	@Override
	public CurrentPatientSession getCurrentUserByUuid(String uuid) throws LoginException {
		
		CurrentPatientSession currentUserSession = sessionDao.findByUuid(uuid);
		
		if(currentUserSession != null) {
			
			return currentUserSession;
			
		}else {
			
			throw new LoginException("Please enter valid key");
		}
	}
	
	// we are refreshing the appointment dates when client is fetching the appointment or client clicking on refresh button
	// from and to will be 24 hours time
	
	public static void getAppointmentDates(Integer from, Integer to) throws IOException, TimeDateException{
		
		// checking from and to is null or not
		
		if(from == null || to == null) {
			 
			throw new TimeDateException("Please enter valid doctor appointment From to To time");
		}
		
		FileReader reader = new FileReader("config.properties");  
	      
	    Properties p = new Properties();  
	    
	    p.load(reader); 
	    
	    LocalDateTime currentDateTime = LocalDateTime.now();
	    
	    LocalDateTime tomorrowDateTime =  currentDateTime.plusDays(1);
	    
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	    
	    // puting todays dates
	    
	    for(int i= from; i <= to; i++) {
	    	
	    	String TodaytimeString = null;
	    	
	    	if(!( i >= 10)) {
	    		
	    		TodaytimeString = currentDateTime.toLocalDate() + " 0" + i + ":00";
	    		
	    	   
	    	}else {
	    		
	    		TodaytimeString = currentDateTime.toLocalDate() + " " + i + ":00";
	    		
	    	}
	    	
	    	LocalDateTime dateTime = LocalDateTime.parse(TodaytimeString, formatter);
	    	
	    	// we are checking if time is gone or not if time is gone then don't put in database
	    	
	    	// 2023-03-09 01:00
	    	
	    	if(currentDateTime.isBefore(dateTime)) {
	    		
	    		myTimeDate.put("today"+i, dateTime);
	    		
	    	}
	    	
	    }
	    
	    // puting tomorrow dates
	    
	    for(int i= from; i <= to; i++) {
	    	
	    	String tomorrowTimeString = null;
	    	
	    	if(!( i >= 10)) {
	    		
	    	   tomorrowTimeString = tomorrowDateTime.toLocalDate() + " 0" + i + ":00";
	    	   
	    	}else {
	    		
	    		tomorrowTimeString = tomorrowDateTime.toLocalDate() + " " + i + ":00";
	    		
	    	}
	    	
	    	LocalDateTime dateTime = LocalDateTime.parse(tomorrowTimeString, formatter);
	    	
	    	// we are checking if time is gone or not if time is gone then don't put in database
	    	if(currentDateTime.isBefore(dateTime)) {
	    		
	    		myTimeDate.put("tomorrow"+i, dateTime);
	    		
	    	}
	    	
	    }
	    
	    
	    
	}

	@Override
	public Appointment bookAppointment(String key, Appointment appointment) throws AppointmentException, LoginException, DoctorException, IOException, TimeDateException {
		
		CurrentPatientSession currentPatientSession = sessionDao.findByUuid(key); 
		
		Optional<Patient> patient = patientDao.findById(currentPatientSession.getUserId());
		
		if(patient.isPresent()) {
			
			// setting patient in appointment
			appointment.setPatient(patient.get());
			
			Doctor doctor = appointment.getDoctor();
			
			
					
			Optional<Doctor> registerDoctors = doctorDao.findById(doctor.getDoctorId());
			
			// setting doctor in appointment
			appointment.setDoctor(registerDoctors.get());
			
			System.out.println("********" + registerDoctors);
			
			if(!registerDoctors.isEmpty()) {
				
				// check if appointment date and time is available or not
				// this line generating time dynamically from doctors choice of work.
				
				getAppointmentDates(registerDoctors.get().getAppointmentFromTime(),registerDoctors.get().getAppointmentToTime());
				
				List<Appointment> listOfAppointment = appointment.getDoctor().getListOfAppointments();
				
				Boolean flag1 = false;
				
				Boolean flag2 = false;
				
				for(Appointment eachAppointment: listOfAppointment) {
					
					
					
					if(eachAppointment.getAppointmentDateAndTime().isEqual(appointment.getAppointmentDateAndTime())) {
						
						flag1 = true;
						
					}
				}
				
				// check if give date and time if correct or not
				
				for(String str : myTimeDate.keySet()) {
					
					System.out.println(myTimeDate.get(str) +  " ** " + appointment.getAppointmentDateAndTime()); 
					
					if(myTimeDate.get(str).isEqual(appointment.getAppointmentDateAndTime())) {
						
						flag2 = true;
						
					}
				}
				
				System.out.println(myTimeDate);
				Appointment registerAppointment = null;
				
				
				
				if(!flag1 && flag2) {
					
					registerAppointment = appointmentDao.save(appointment);
					
				}else {
					
					throw new AppointmentException("This time or date already booked. Please enter valid appointment time and date " + appointment.getAppointmentDateAndTime());
					
				}
				
				
				// we can't map appointment object directly because we don't have appointment id in it we have to mapped after saving the 
				// appointment and then we will get the appointment id then it will not generate appointment again. If we mapped the register
				// appointment.
				
				// mapping appointment in doctor and then saving doctor
				
				
				
				registerDoctors.get().getListOfAppointments().add(registerAppointment);
				
				doctorDao.save(registerDoctors.get());
				
				// mapping appointment in patient then saving patient
				
				patient.get().getListOfAppointments().add(registerAppointment);
				
				patientDao.save(patient.get());
				
				return registerAppointment;
				
				
			}else {
				 
				throw new DoctorException("Please enter valid doctors details or doctor not present with thid id " + doctor.getDoctorId());
				
			}
			
			
			
		}else {
			
			throw new LoginException("Please enter valid key"); 
			
		}
	}

	@Override
	public List<Appointment> getAllAppointmenPatientWise(String key) throws AppointmentException, PatientException {
		
		
		
		CurrentPatientSession currentPatientSession = sessionDao.findByUuid(key); 
		
		Optional<Patient> patient = patientDao.findById(currentPatientSession.getUserId());
		
		if(patient.get() != null) {
			
			List<Appointment> listOfAppointments = patient.get().getListOfAppointments();
			
			if(!listOfAppointments.isEmpty()) {
				
				return listOfAppointments;
				
			}else {
				
				throw new AppointmentException("No appointments found. Please book appointments");
			}
			
		}else {
			
			throw new PatientException("Please enter valid patient details");
		}
	}

}









































