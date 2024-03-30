CALL mvnw clean compile assembly:single
CALL java -cp target/googol-1.0-SNAPSHOT.jar hgnn.QueueManager 3 https://www.geeksforgeeks.org/killing-threads-in-java/