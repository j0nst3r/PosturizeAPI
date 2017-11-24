package edu.sjsu.posturize.api;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.sjsu.posturize.api.firebase.FirebaseConfig;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/home")
@PropertySource("classpath:application.properties")
public class PosturizeController {


	@Value("${firestore.credential}")
    private String credential;
	
	@Value("${firestore.credentialPath}")
    private String credentialPath;
    
    @Value("${firestore.projectId}")
    private String projectId;
	
    @RequestMapping(path = "/healthCheck", method = RequestMethod.GET)
    @ResponseBody
    public String healthCheck(){
       return "SERVER UP AND RUNNING";
    }

    @RequestMapping(path = "/resourceCheck", method = RequestMethod.GET)
    @ResponseBody
    public String resourceCheck(){
    	//Firestore db = firestoreDatabase()
    	//return db != null ? "DB connected" : "DB not connected";
    	
    	FirestoreOptions firestoreOptions =
    	    FirestoreOptions.getDefaultInstance().toBuilder()
    	        .setProjectId(projectId)
    	        .build();
    	Firestore db = firestoreOptions.getService();
    	
    	return (db != null) ? "DB CONNECTED" : "DB NOT CONNECTED"; 
    	
    }
    
    @RequestMapping(path = "/firestore", method = RequestMethod.GET)
    @ResponseBody
    public String test() throws InterruptedException, ExecutionException, IOException{
    	InputStream is = new ClassPathResource(credentialPath).getInputStream();
    	FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
    			//.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(System.getenv(credential))))
    			.setCredentials(ServiceAccountCredentials.fromStream(is))
                .setProjectId(projectId).build();
        Firestore db = firestoreOptions.getService();
        
	    //gets the user collection and then get a list of documents within the collection
	    Query temp = db.collection("users");
		ApiFuture<QuerySnapshot> collections = temp.get();
		for(DocumentSnapshot doc : collections.get().getDocuments()){
			System.out.println(doc.getId());
		}
		return "done executing code....";    
    }
    
    @RequestMapping(path = "/populateTest", method = RequestMethod.GET)
    @ResponseBody
    public String populateTest() throws InterruptedException, ExecutionException, IOException{
    	InputStream is = new ClassPathResource(credentialPath).getInputStream();
    	FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
    			//.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(System.getenv(credential))))
    			.setCredentials(ServiceAccountCredentials.fromStream(is))
                .setProjectId(projectId).build();
        Firestore db = firestoreOptions.getService();
        
	    //updates needSync value to true(force dailyanaylsis to be performed)
        DocumentReference docRefDev = db.collection("users").document("DEV-Test");
        Map<String, Object> dataDev = new HashMap<>();
    	dataDev.put("needSync", true);
    	ApiFuture<WriteResult> resultDev = docRefDev.update(dataDev);
    	
    	DocumentReference docRefQA = db.collection("users").document("QA-Test");
        Map<String, Object> dataQA = new HashMap<>();
    	dataQA.put("needSync", true);
    	ApiFuture<WriteResult> resultQA = docRefQA.update(dataQA);
		
    	//Generate dummy data
    	ArrayList<Double> slouchesDev = new ArrayList<>();
    	ArrayList<Date> timesDev = new ArrayList<>();
    	ArrayList<Double> slouchesQA = new ArrayList<>();
    	ArrayList<Date> timesQA = new ArrayList<>();
    	for(int a = 0; a < 20; a++){
    		Date curDate = new Date();
    		Random rando = new Random(System.currentTimeMillis());
    		
    		timesDev.add(curDate);
    		timesQA.add(curDate);
    		slouchesDev.add(rando.nextDouble()*-1*5);
    		slouchesQA.add(rando.nextDouble()*-1*5);
    		
    		Thread.sleep(1000);
    	}
    	
    	//updates slouches with generated data
    	docRefDev = db.collection("slouches").document("DEV-Test");
        dataDev = new HashMap<>();
        dataDev.put("times", timesDev);
        dataDev.put("slouches", slouchesDev);
    	resultDev = docRefDev.update(dataDev);
    	
    	docRefQA = db.collection("slouches").document("QA-Test");
        dataQA = new HashMap<>();
        dataQA.put("times", timesQA);
        dataQA.put("slouches", slouchesQA);
    	resultQA = docRefQA.update(dataQA);
    	
		
		return "Finished populate Dev-Test and QA-Test";    
    }
    
    
    
    
    @RequestMapping(path = "/forceAnalysis/{userId}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> firebase(@PathVariable String userId) throws InterruptedException, ExecutionException, IOException{
		HttpHeaders httpHeaders = new HttpHeaders();
		String resultData = FirebaseConfig.forcedAnalysis(userId);	
			
		return new ResponseEntity<String>(resultData, httpHeaders, HttpStatus.OK);
    }
}