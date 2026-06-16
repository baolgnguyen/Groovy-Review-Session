import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

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

// Q1 — Parse & Clean the Data [Logic + Closures]
// Closure to parse the raw test results
def parseResults = { List<String> lines ->
	def cleanList = []
	def skippedCount = 0
	def parsedCount = 0

	lines.each { line ->
		// Use standard regex escape for the pipe character
		def parts = line.split('\\|')
		def len = parts.length

		// Validate layout: Must be 6 fields (PASS/SKIP) or 7 fields (FAIL)
		if (len != 6 && len != 7) {
			skippedCount++ // Count how many rows were skipped
		} else {
			parsedCount++
			// Build and append the map to the clean list
			cleanList << [
				id       : parts[0],
				module   : parts[1],
				suiteType: parts[2],
				status   : parts[3],
				duration : parts[4].toDouble(), // Convert duration to a Double
				browser  : parts[5],
				error    : (len == 7) ? parts[6] : null // FAIL rows have a 7th field; PASS/SKIP have null
			]
		}
	}

	// Print how many rows were parsed vs skipped exactly as expected
	println "Parsed: ${parsedCount} valid rows | Skipped: ${skippedCount} invalid rows"

	return cleanList
}

// Execute the parser
def cleanedResults = parseResults(rawResults)

// Pretty print the output
cleanedResults.each { println it }

// Q2 — Compute Statistics  [Closures + Collection Methods]
// (a) Overall: total tests, pass count, fail count, skip count
def passTC = cleanedResults.findAll{it.status == "PASS"}.size()
def failTC = cleanedResults.findAll{it.status == "FAIL"}.size()
def skipTC = cleanedResults.findAll{it.status == "SKIP"}.size()
def totalTC = cleanedResults.size()

println "a. Total: ${totalTC}, Pass: ${passTC}, Fail: ${failTC}, Skip: ${skipTC}"

// (b) Pass rate = pass / (total − skip), as a percentage with 1 decimal
def passRate = ((passTC / (totalTC - skipTC))*100).round(1)
println "b. Pass rate: ${passRate}"

// (c) Total and average duration (average over non-skipped tests, 2 decimals)
def totalDuration = cleanedResults.findAll{it.status != "SKIP"}.sum{it.duration}
def avgDuration = (totalDuration / totalTC).round(2)

println "c. Total Duration: ${totalDuration}, Avg Duration: ${avgDuration}"

// (d) Count of tests per module → [Login:3, Search:3, Checkout:3, Profile:3]
def tcPerModule = cleanedResults.groupBy{it.module}.collectEntries{module, size -> [module, size.size()]}
println "d. ${tcPerModule}"

// (e) Pass rate per browser → [chrome:'57.1%', firefox:'100.0%']  (exclude skips from the denominator)
def tcPerBrowser = cleanedResults.groupBy{it.browser}

def passTC2 = tcPerBrowser.chrome.findAll{it.status == "PASS"}.size()
def failTC2 = tcPerBrowser.chrome.findAll{it.status == "FAIL"}.size()
def skipTC2 = tcPerBrowser.chrome.findAll{it.status == "SKIP"}.size()
def totalTC2 = tcPerBrowser.chrome.size()
def passRatePerChrome = ((passTC2 / (totalTC2 - skipTC2))*100).round(1)

def passTC3 = tcPerBrowser?.firefox?.findAll{it.status == "PASS"}?.size() ?: 0
def failTC3 = tcPerBrowser?.firefox?.findAll{it.status == "FAIL"}?.size() ?: 0
def skipTC3 = tcPerBrowser?.firefox?.findAll{it.status == "SKIP"}?.size() ?: 0
def totalTC3 = tcPerBrowser?.firefox?.size() ?: 0

def passRatePerFirefox = 0
if (totalTC3 != 0 || passTC3 != 0) {
	passRatePerFirefox = ((passTC3 / (totalTC3 - skipTC3)) * 100).round(1)
}

println "e. Chrome: ${passRatePerChrome}, Firefox: ${passRatePerFirefox}"
