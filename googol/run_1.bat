rem run 1 Barrel, 1 Downloader, Gateway

call mvnw clean compile assembly:single
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Queue
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.RMIGateway
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Downloader 1 0
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Barrel Barrel-1 0