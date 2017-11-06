package edu.sjsu.posturize.api;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
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
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

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
    
        DocumentReference docRef = db.collection("users").document("alovelace");
	     // Add document data  with id "alovelace" using a hashmap
	     Map<String, Object> data = new HashMap<>();
	     data.put("first", "Ada");
	     data.put("last", "Lovelace");
	     data.put("born", 1815);
	     //asynchronously write data
	     ApiFuture<WriteResult> result = docRef.set(data);
	     // ...
	     // result.get() blocks on response
	     System.out.println("Update time : " + result.get().getUpdateTime());
        
        // [START fs_get_doc_as_map]
	    docRef = db.collection("users").document("UnJhPIzXSBWlBYKBJD2I");
	    // asynchronously retrieve the document
	    ApiFuture<DocumentSnapshot> future = docRef.get();
	    // ...
	    // future.get() blocks on response
	    DocumentSnapshot document = future.get();
	    if (document.exists()) {
	      System.out.println("Document data: " + document.getData());
	    } else {
	      System.out.println("No such document!");
	    }
	    // [END fs_get_doc_as_map]
	    return (document.exists()) ? document.getData().toString() : null;
    }
    
    @RequestMapping(path = "/firebase", method = RequestMethod.GET)
    @ResponseBody
    public String firebase() throws InterruptedException, ExecutionException, IOException{
		// As an admin, the app has access to read and write all data, regardless of Security Rules
		DatabaseReference ref = FirebaseDatabase
			    .getInstance()
			    .getReference("users");
			ref.addListenerForSingleValueEvent(new ValueEventListener() {
			    @Override
			    public void onDataChange(DataSnapshot dataSnapshot) {
			        Object document = dataSnapshot.getValue();
			        System.out.println(document);
			    }

				@Override
				public void onCancelled(DatabaseError e) {
					// TODO Auto-generated method stub
				}
			});
		return "testing...";
    }
}