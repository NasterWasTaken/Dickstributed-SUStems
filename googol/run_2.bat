REM run 1 Donwloader, 1 Barrel, user

CALL mvnw clean compile assembly:single
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Downloader 1 1
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.Barrel Barrel-2 1
start cmd /c call java -cp target/googol-1.0-SNAPSHOT.jar hgnn.RMIClient