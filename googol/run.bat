CALL mvnw clean compile assembly:single
CALL java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Downloader https://nier.fandom.com/wiki/NieR_Wiki