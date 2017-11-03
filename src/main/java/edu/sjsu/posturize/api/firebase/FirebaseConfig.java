package edu.sjsu.posturize.api.firebase;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//import com.google.auth.oauth2.GoogleCredentials;

@Configuration
@PropertySource("classpath:application.properties")
public class FirebaseConfig {

	@Bean
	  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
	      return new PropertySourcesPlaceholderConfigurer();
	  }
	
    @Bean
    public DatabaseReference firebaseDatabse() {
        DatabaseReference firebase = FirebaseDatabase.getInstance().getReference();
        return firebase;
    }
    
    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Value("${firebase.config.path}")
    private String configPath;
    
    @PostConstruct
	public void init() throws FileNotFoundException, IOException {
    	//SET UP FIREBASE CONNECTION
		InputStream inputStream = FirebaseConfig.class.getClassLoader().getResourceAsStream(configPath);

		FirebaseOptions options = new FirebaseOptions.Builder().setServiceAccount(inputStream)
				.setDatabaseUrl(databaseUrl).build();
		FirebaseApp.initializeApp(options);
	}
}