# HttpClient

```kotlin
val scope = rememberCoroutineScope()
var job: Job? by remember { mutableStateOf(null) }
val url = ""

if (job == null || job!!.isCompleted || job!!.isCancelled) {
    job = scope.launch {
        HttpClient.Get(url) { code, data ->
            if (code == HttpClient.OK) {
                val text = String(data, Charsets.UTF_8)
                // ...
            }
        }
    }
}
```
