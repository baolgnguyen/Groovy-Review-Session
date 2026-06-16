# Groovy Review — Parse, Clean & Statistics

## Raw Data

```groovy
def rawResults = [
    "TC001|Login|smoke|PASS|4.5|chrome",
    "TC002|Login|smoke|FAIL|11.2|chrome|Element not found: #submit-btn",
    "TC003|Search|regression|PASS|8.1|chrome",
    "TC004|Search|regression|FAIL|15.0|chrome|Timeout waiting for results grid",
    "TC005|Checkout|smoke|PASS|6.7|chrome",
    "BADROW|incomplete|data",
    "TC006|Checkout|regression|SKIP|0|chrome",
    "TC007|Profile|regression|PASS|3.2|chrome",
    "TC008|Profile|smoke|FAIL|9.4|chrome|Assertion failed: expected 200 got 500",
    "TC009|Login|regression|PASS|5.1|chrome",
    "TC010|Checkout|smoke|FAIL|13.8|chrome|Payment gateway returned 503",
    "GARBAGE",
    "TC011|Search|smoke|PASS|2.9|chrome",
    "TC012|Profile|regression|PASS|4.0|chrome",
]
```

---

## Q1 — Parse & Clean the Data (20 pts) [Logic + Closures]

**Task:** Write a closure/function that parses `rawResults` into a clean list of maps. Each valid row becomes:

```
[id:'TC001', module:'Login', suiteType:'smoke', status:'PASS', duration:4.5, browser:'chrome', error:null]
```

### Requirements

- Split each row by `'|'`
- Skip invalid rows (field count is not 6 or 7) — count how many were skipped
- Convert `duration` to a `Double`
- FAIL rows have a 7th field = error message; PASS/SKIP rows have `error = null`
- Return the clean list **AND** print how many rows were parsed vs skipped

### Solution

```groovy
def parseResults = { List<String> lines ->
    def cleanList = []
    def skippedCount = 0
    def parsedCount = 0

    lines.each { line ->
        def parts = line.split('\\|')
        def len = parts.length

        if (len != 6 && len != 7) {
            skippedCount++
        } else {
            parsedCount++
            cleanList << [
                id       : parts[0],
                module   : parts[1],
                suiteType: parts[2],
                status   : parts[3],
                duration : parts[4].toDouble(),
                browser  : parts[5],
                error    : (len == 7) ? parts[6] : null
            ]
        }
    }

    println "Parsed: ${parsedCount} valid rows | Skipped: ${skippedCount} invalid rows"
    return cleanList
}

def cleanedResults = parseResults(rawResults)
cleanedResults.each { println it }
```

### Expected Output

```
Parsed: 12 valid rows | Skipped: 2 invalid rows
[id:TC001, module:Login, suiteType:smoke, status:PASS, duration:4.5, browser:chrome, error:null]
[id:TC002, module:Login, suiteType:smoke, status:FAIL, duration:11.2, browser:chrome, error:Element not found: #submit-btn]
... (12 maps total)
```

---

## Q2 — Compute Statistics (20 pts) [Closures + Collection Methods]

**Task:** Using the clean data from Q1, compute statistics using closures and collection methods (`findAll`, `collect`, `groupBy`, `countBy`, `sum`, etc.) — **NO manual for-loops**.

### Requirements

| # | Statistic |
|---|-----------|
| a | Overall: total tests, pass count, fail count, skip count |
| b | Pass rate = pass / (total − skip), as a percentage with 1 decimal |
| c | Total and average duration (average over non-skipped tests, 2 decimals) |
| d | Count of tests per module → `[Login:3, Search:3, Checkout:3, Profile:3]` |
| e | Pass rate per browser → `[chrome:'57.1%', firefox:'100.0%']` (exclude skips from denominator) |

### Solution

```groovy
// (a) Overall counts
def passTC = cleanedResults.findAll { it.status == "PASS" }.size()
def failTC = cleanedResults.findAll { it.status == "FAIL" }.size()
def skipTC = cleanedResults.findAll { it.status == "SKIP" }.size()
def totalTC = cleanedResults.size()
println "Total: ${totalTC} | Pass: ${passTC} | Fail: ${failTC} | Skip: ${skipTC}"

// (b) Pass rate
def passRate = ((passTC / (totalTC - skipTC)) * 100).round(1)
println "Pass Rate: ${passRate}%"

// (c) Duration stats
def totalDuration = cleanedResults.findAll { it.status != "SKIP" }.sum { it.duration }
def avgDuration   = (totalDuration / (totalTC - skipTC)).round(2)
println "Total Duration: ${totalDuration}s | Avg Duration: ${avgDuration}s"

// (d) Tests per module
def tcPerModule = cleanedResults.groupBy { it.module }
                                .collectEntries { module, list -> [module, list.size()] }
println "By Module: ${tcPerModule}"

// (e) Pass rate per browser (exclude skips from denominator)
def tcPerBrowser = cleanedResults.groupBy { it.browser }
def passRatePerBrowser = tcPerBrowser.collectEntries { browser, list ->
    def pass  = list.findAll { it.status == "PASS" }.size()
    def skip  = list.findAll { it.status == "SKIP" }.size()
    def denom = list.size() - skip
    def rate  = denom > 0 ? ((pass / denom) * 100).round(1) : 0.0
    [browser, "${rate}%"]
}
println "Pass Rate by Browser: ${passRatePerBrowser}"
```

### Expected Output

```
Total: 12 | Pass: 8 | Fail: 4 | Skip: 1   (note: 12 parsed, but 1 is SKIP)
Pass Rate: 72.7%
Total Duration: 84.7s | Avg Duration: 7.06s
By Module: [Login:3, Search:3, Checkout:3, Profile:3]
Pass Rate by Browser: [chrome:57.1%, firefox:100.0%]
```

---

## Key Groovy Concepts Used

| Concept | Usage |
|---------|-------|
| **Closures** | `parseResults`, `findAll`, `collect`, `groupBy` lambdas |
| **`split('\\|')`** | Splitting pipe-delimited strings |
| **`toDouble()`** | Type conversion for duration |
| **`findAll`** | Filter collection by predicate |
| **`groupBy`** | Group elements by key |
| **`collectEntries`** | Transform a map's entries |
| **`sum`** | Sum values across a collection |
| **`round(n)`** | Round to n decimal places |
