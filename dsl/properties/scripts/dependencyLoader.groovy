import java.util.zip.*
import java.security.MessageDigest
import groovy.json.JsonOutput

def path = args.path
def chunkSize = args.chunkSize ?: 1024 * 1024
def number = args.counter ?: 0
File lib = new File(path)

def files = []
lib.eachFileRecurse { f ->
    if (f.isFile()) {
        files << f
    }
}

def encodedFiles = [:]
def fileList = []
files.sort { a, b -> a.absolutePath <=> b.absolutePath }.each { f ->
    def relPath = lib.toURI().relativize(f.toURI()).toString()
    fileList << relPath
    def base64 = f.bytes.encodeBase64().toString()
    encodedFiles[relPath] = base64
}

def result = JsonOutput.toJson(encodedFiles).getBytes().encodeBase64().toString()
def length = result.length()

def start = number * chunkSize
def end = (number + 1) * chunkSize - 1
def hasMore = true
if (end >= length) {
    end = length - 1
    hasMore = false
}

def chunk = result[start .. end]
return [
  chunk: chunk,
  chunkLength: chunk.length(),
  length: length,
  hasMore: hasMore,
  checksum: generateMD5(result.bytes),
  files: fileList
]

// Older version of Groovy does not have String.md5()
def generateMD5(byte[] bytes) {
   MessageDigest digest = MessageDigest.getInstance("MD5")
   digest.update(bytes)
   byte[] md5sum = digest.digest()
   BigInteger bigInt = new BigInteger(1, md5sum)
   return bigInt.toString(16).padLeft(32, '0')
}
