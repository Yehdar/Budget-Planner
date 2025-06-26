Running Code:

```

```

- Backend

```
 ./gradlew clean run
```

if things don't work

```
./gradlew --status
./gradlew --stop
```

- Database

```
  sudo -u postgres psql
  \c budget_db
```

Installation:

- Frontend

```
  cd frontend
  npm create vite@latest . -- --template react-ts
  npm install
  npm install axios
```

- Backend

```
  mkdir backend
  cd backend
  gradle init # ends up using Groovy DSL instead of Kotlin DSL. Kotlin DSL is generated through an IDE
```

- Database

```
  sudo service postgresql start
  sudo -u postgres psql
  \c budget_db
```

Build Tips:

```
./gradlew --stop
./gradlew clean build run
```

Challenges:

- Gradle Version Outdated
  I'm building this application raw in bash (I am using VS Code as the text editor, but I'm building and running the components in the terminal). I didn't have gradle installed on my system so I `sudo apt install gradle`, but that downloaded version 4.4.1, which cannot recognize Java 21 (gradle 7+) and modern ktor/kotlin plugins will fail (gradle 8.3+). Without realizing this mistake, I went ahead to build the backend with `.gradlew run`. Obviously, it didn't work, so I had to `rm -rf gradle gradlew gradlew.bat` after. Additionally, I `sudo apt remove gradle` to start clean. By the way, I originally planned to downgrade Java to version 17, but that didn't work (and it'd be a bad decision). As such, I had to get the latest version of Gradle by `cd ~`, `wget https://services.gradle.org/distributions/gradle-8.5-bin.zip`, `unzip gradle-8.5-bin.zip`, and `sudo mv gradle-8.5 /opt/gradle`. Then, I added Gradle 8.5 to my PATH by editing my .bashrc via `nano ~/.bashrc` with `export PATH=/opt/gradle/bin:$PATH` at the bottom. Following this, I saved the file and ran `source ~/.bashrc`. In the same terminal, I did `gradle -v` and it worked! So I went over to ./backend/ to `gradle wrapper --gradle-version 8.5` and did `.gradlew run`
- PostgreSQL Version Outdated
  Got the error "template1 has a collation version mismatch
  created using collation version 2.37, but OS provides version 2.39" after trying `CREATE DATABASE budget_db; in psql. Not my fault, it's a known issue caused by psql after an OS update. Fixed it by refreshing the collation version.
`
