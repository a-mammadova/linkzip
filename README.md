# linkzip
linkzip is the most original idea ever (after calculator) -> url shotener... 
it is a *tiny* project im developing to understand http and networking fundamentals better

for now there's nothing fancy yet

# stack
- java (sockets)
- http/1.1
- collections (`ConcurrentHashMap` but will replace with a normal db, soon)

# running locally

**requirements**: jdk

1. clone the repo
```
git clone https://github.com/a-mammadova/linkzip.git
cd linkzip
```

2. compile
```
javac -d out src/*.java
```

3. run
```
java -cp out Main
```
4. try it
```
curl -X POST http://localhost:8080/zip -d "https://www.reddit.com/r/TheMachineLearning/s/3FeFqj7nFz"
```

note: not recommended for production use. 
