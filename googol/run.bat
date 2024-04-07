CALL mvnw clean compile assembly:single
start cmd /c CALL java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Queue
start cmd /c CALL java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Downloader 3
start cmd /c CALL java -cp target/googol-1.0-SNAPSHOT.jar hgnn.BarrelMulticastTest