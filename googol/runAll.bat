call mvnw clean compile assembly:single
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Queue
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.RMIGateway