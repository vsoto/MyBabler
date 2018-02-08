
package edu.columbia.main.db;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gideon on 9/22/15.
 */

/**
 * Singleton for MongoDB Connection
 */
public enum MongoDB {

    INSTANCE;
    MongoClient mongoClient;

    MongoDB(){
        mongoClient = new MongoClient(new ServerAddress("127.0.0.1",27017));
    }

    public MongoDatabase getDatabase(String dbName){
        return mongoClient.getDatabase(dbName);
    }

    public void setClient(MongoClient client) {
        this.mongoClient = client;
    }
}

