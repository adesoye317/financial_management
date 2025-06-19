package com.financal.mgt.Financal.Management.config;

import com.financal.mgt.Financal.Management.util.EncryptionHelper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class SpringMongoConfig {
	private static final Logger log = LoggerFactory.getLogger(SpringMongoConfig.class);


	@Value("${spring.data.mongodb.database}")
	private String databaseName;

	@Value("${spring.data.mongodb.username}")
	private String userName;

	@Value("${spring.data.mongodb.password}")
	private String password;

	@Value("${spring.data.mongodb.cluster-url}")
	private String clusterUrl; // e.g., cluster0.duuj5fu.mongodb.net

	@Bean
	@Primary
	public MongoClient mongo() {
		try {
			// Decrypt credentials
			String decryptedUsername = EncryptionHelper.decrypt(userName);
			String decryptedPassword = EncryptionHelper.decrypt(password);

			log.info("Decrypted MongoDB Username: {}", decryptedUsername);

			// Construct the MongoDB SRV URI
			String srvUri = String.format(
					"mongodb+srv://%s:%s@%s/?retryWrites=true&w=majority&appName=Cluster0",
					decryptedUsername, decryptedPassword, clusterUrl
			);

//			log.info("MongoDB SRV connection URI: {}", srvUri);

			// Create the MongoClient
			ConnectionString connectionString = new ConnectionString(srvUri);
			MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
					.applyConnectionString(connectionString)
					.build();

			return MongoClients.create(mongoClientSettings);
		} catch (Exception e) {
			log.error("Failed to create MongoClient: ", e);
			throw e;
		}
	}

	@Bean
	public MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongo(), databaseName);
	}
}
