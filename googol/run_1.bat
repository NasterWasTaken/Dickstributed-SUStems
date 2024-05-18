rem run 1 Barrel, 1 Downloader, Gateway

call mvnw clean compile assembly:single
start cmd /k call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Queue
start cmd /k call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.RMIGateway
start cmd /k call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Downloader 1 0
start cmd /k call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Barrel Barrel-1 0