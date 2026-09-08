You are working on a file-conversion application with separate Audio, Video, and Image tabs and a backend that currently contains separate files/functions for different formats.

Your task is NOT to perform a superficial code review.

You must perform an exhaustive audit of the ENTIRE codebase, understand the existing architecture, identify every logical break in the conversion pipeline, and then FIX the backend so that the conversion system behaves consistently and reliably across all supported formats.

Take as much time as necessary. Do not stop after finding the first few bugs. Use as many available skills, tools, repository searches, static-analysis techniques, test techniques, and shell commands as necessary. The objective is a COMPLETE working conversion backend, not merely a list of suspected problems.

IMPORTANT:
Do not assume that a conversion works because a function exists.
Do not assume that a conversion works because FFmpeg is imported.
Do not assume that an output file exists because a path was constructed.
Do not assume that asynchronous code works because it returns a Promise/Future.
Actually trace and, where possible, execute the complete conversion path.

==================================================
1. FIRST: SCAN THE ENTIRE CODEBASE
==================================================

Before modifying anything:

1. Recursively inspect the entire repository.
2. Identify:
   - all backend files
   - all conversion modules
   - all format-specific functions
   - all format registries/mappings
   - all input/output path logic
   - all FFmpeg invocation code
   - all other external conversion tools
   - all async/task/worker/queue logic
   - all frontend-to-backend conversion requests
   - all backend-to-frontend result/error handling
   - all temporary-file handling
   - all output-directory handling
   - all validation logic
   - all MIME/extension detection
   - all tests
   - all configuration files
3. Build a mental/model representation of the complete conversion architecture before making major changes.

Do not focus only on Audio.

Audit:

AUDIO
VIDEO
IMAGE
and any other conversion category present in the application.

Search the repository for every occurrence of:
- ffmpeg
- ffprobe
- ImageMagick
- convert
- magick
- format
- extension
- MIME
- output
- input
- conversion
- convertFile
- convertFiles
- async
- await
- worker
- queue
- process
- spawn
- exec
- temporary files
- destination paths

Also inspect indirect/dynamic invocation mechanisms rather than relying only on keyword searches.

==================================================
2. MAP EVERY SUPPORTED FORMAT
==================================================

Find the authoritative list of supported formats in the application.

Do not invent a new format list unless the current architecture genuinely lacks one.

Create an internal conversion matrix.

For every supported source format and every supported destination format, determine:

SOURCE → DESTINATION

For example:

Audio:

MP3 → MP3
MP3 → M4A
MP3 → FLAC
MP3 → WAV
MP3 → ALAC
MP3 → OGG
MP3 → OPUS
...

M4A → MP3
M4A → FLAC
M4A → WAV
M4A → ALAC
...

FLAC → MP3
FLAC → M4A
...

And similarly for every supported video and image format.

Do NOT assume that source and destination being in the same category automatically means the conversion works.

The goal is:

If the UI allows the user to select FORMAT A as the input and FORMAT B as the output, the backend must actually support that path.

If a conversion is fundamentally impossible or unsupported by the underlying tool, the application must explicitly reject it with a correct error rather than silently failing.

There must be no situation where the UI advertises a conversion that the backend cannot perform.

==================================================
3. TRACE EVERY CONVERSION PATH
==================================================

For EVERY format pair, trace the complete execution path:

Frontend selection
    ↓
conversion request
    ↓
backend routing
    ↓
format detection
    ↓
conversion function
    ↓
input validation
    ↓
converter invocation
    ↓
arguments/options
    ↓
process execution
    ↓
exit-code handling
    ↓
stderr/stdout handling
    ↓
output-file creation
    ↓
output-file validation
    ↓
output-directory handling
    ↓
result returned to frontend

Look specifically for logical breaks such as:

- wrong function selected
- incorrect format lookup
- case-sensitive extension problems
- MIME type mismatch
- incorrect destination extension
- incorrect FFmpeg codec
- missing FFmpeg arguments
- wrong FFmpeg argument ordering
- incorrect input path
- incorrect output path
- output directory not existing
- output file overwritten unexpectedly
- asynchronous process not awaited
- Promise/Future resolving before FFmpeg finishes
- process errors being swallowed
- stderr being ignored
- non-zero exit codes treated as success
- success returned when output does not exist
- temporary files removed too early
- temporary files never removed
- race conditions
- concurrent conversions interfering with one another
- batch conversions sharing mutable state
- incorrect filename handling
- filenames containing spaces
- filenames containing Unicode characters
- filenames containing special characters
- duplicate filenames
- extension handling bugs
- relative-vs-absolute path bugs
- platform-specific path bugs
- incorrect shell escaping
- shell command injection risks
- missing executable detection
- FFmpeg unavailable/not found
- wrong FFmpeg binary
- FFmpeg capability assumptions
- unsupported codec/container combinations
- incorrect audio stream selection
- incorrect video stream selection
- metadata loss where preservation is expected
- output being written somewhere other than the configured output directory
- output path being returned before the file exists
- errors being converted into generic success responses
- one conversion implementation behaving differently from another for no architectural reason

==================================================
4. VERIFY THAT FFmpeg IS ACTUALLY BEING CALLED
==================================================

For every conversion implementation that claims to use FFmpeg:

Verify the actual process invocation.

Do not merely inspect something like:

ffmpeg(...)
or
spawn("ffmpeg"...)

Trace the actual execution.

Verify:

- executable resolution
- complete argument list
- input file
- output file
- codecs
- containers
- overwrite behavior
- error handling
- process completion
- exit code
- output existence

Where the environment permits, execute representative conversions and inspect the resulting process behavior.

If the application has an abstraction around FFmpeg, inspect both the abstraction and every caller.

If FFmpeg is being invoked incorrectly, fix the underlying abstraction rather than creating one-off patches for individual formats.

==================================================
5. VERIFY THAT OUTPUT FILES REALLY EXIST
==================================================

A conversion is NOT successful merely because the converter process returned or a function resolved.

For every successful conversion:

1. FFmpeg/other converter must complete.
2. Exit status must indicate success.
3. Expected output path must exist.
4. Output must be a regular file.
5. Output must have a non-zero/valid size.
6. Where practical, validate that the output can actually be parsed/opened by the appropriate tool.
7. The backend must return the actual output path/result only after these checks pass.

If any of these conditions fail, the conversion must be considered failed.

Never return a false success.

==================================================
6. SINGLE-FILE CONVERSION
==================================================

Explicitly verify:

ONE INPUT FILE → ONE SELECTED OUTPUT FORMAT

Test this across every supported format combination that is supposed to work.

Examples include:

M4A → ALAC
OGG → OPUS
MP3 → FLAC
FLAC → MP3
WAV → M4A
etc.

Do not limit testing to these examples. They are examples of the class of bugs that must be discovered.

The same applies to Video and Image conversions.

==================================================
7. MULTIPLE FILES → ONE OUTPUT FORMAT
==================================================

Explicitly verify the batch workflow:

INPUT 1
INPUT 2
INPUT 3
...
        ↓
SELECT ONE OUTPUT FORMAT
        ↓
OUTPUT 1
OUTPUT 2
OUTPUT 3
...

Every input must be processed independently.

Verify:

- each input receives its own conversion task
- tasks do not overwrite one another
- filenames remain unique
- output extensions are correct
- output directory is correct
- failures in one file do not incorrectly mark unrelated files as successful
- async execution is correctly awaited
- concurrent processing does not introduce race conditions
- completion reporting is accurate
- partial failures are represented correctly
- all expected output files actually exist

Test both sequential and concurrent behavior if the application supports concurrency.

==================================================
8. ASYNCHRONOUS EXECUTION
==================================================

The existing system intentionally uses asynchronous conversion functions.

Audit this very carefully.

For every async conversion path determine:

- Who creates the task?
- Who awaits it?
- Who catches the exception?
- When does the task resolve?
- Does it resolve only after the converter exits?
- Does it resolve only after output validation?
- What happens if FFmpeg fails?
- What happens if two conversions run simultaneously?
- Are temporary/output paths unique?
- Is shared state being mutated?
- Can one task accidentally resolve another task?
- Can a failed task be reported as successful?

Fix async architecture problems at their source.

Do not simply add random `await` statements without understanding the execution model.

==================================================
9. DO NOT PATCH INDIVIDUAL FORMAT PAIRS ONE BY ONE
==================================================

This is extremely important.

If you discover:

M4A → ALAC is broken

do NOT merely add a special-case fix for M4A → ALAC.

Determine why the conversion architecture allows one pair to fail while another succeeds.

If the root problem is:

- codec mapping
- container mapping
- routing
- output-extension handling
- FFmpeg abstraction
- argument construction
- format registry
- asynchronous process management
- output validation

fix that shared system.

The resulting architecture should make new supported conversions predictable rather than requiring another special-case function.

Preserve the user's existing architectural intent where it is sound, but simplify duplicated logic when duplication itself is causing inconsistencies.

==================================================
10. FORMAT-SPECIFIC IMPLEMENTATIONS
==================================================

The current project intentionally contains separate files/functions for different formats.

Do not blindly delete this architecture.

First determine whether those separate modules are actually necessary.

Where separate functions are useful, make them reliable.

Where duplicated implementations are causing inconsistent behavior, introduce a shared conversion layer while preserving the public interfaces expected by the rest of the application.

The final architecture should have:

- a clear source-format detection mechanism
- a clear destination-format mechanism
- a reliable conversion dispatcher
- centralized process execution
- centralized output validation
- consistent async behavior
- consistent error handling
- format-specific codec/container configuration where necessary
- no accidental format-pair gaps

==================================================
11. AUDIO
==================================================

Audit every supported audio format.

Pay special attention to:

- MP3
- M4A
- AAC
- ALAC
- FLAC
- WAV
- OGG
- OPUS
- AIFF
- any other audio format actually supported by the application

Verify container/codec correctness.

For example, do not confuse:

container format
with
audio codec.

A destination extension alone does not guarantee that the resulting file contains the requested codec.

Verify the actual resulting media properties using FFprobe or an equivalent mechanism where appropriate.

If metadata preservation is part of the application's intended behavior, verify it too.

==================================================
12. VIDEO
==================================================

Audit every supported video format.

Verify:

- container
- video codec
- audio codec
- stream mapping
- resolution handling
- frame rate handling
- audio preservation
- subtitle behavior where applicable
- metadata behavior where applicable
- output extension
- output validation

Do not assume a video conversion succeeded simply because FFmpeg created a file.

==================================================
13. IMAGE
==================================================

Audit every supported image format and its actual conversion backend.

Verify:

- input decoding
- output encoding
- extension
- MIME type
- quality/compression parameters
- transparency/alpha handling
- color profile behavior where relevant
- output existence
- output validity

If ImageMagick or another tool is used, verify its invocation and exit status just as rigorously as FFmpeg.

==================================================
14. ERROR HANDLING
==================================================

Find every location where conversion errors can be swallowed.

Bad patterns include:

- empty catch blocks
- logging an error but returning success
- returning null/undefined and treating it as success
- ignoring process exit codes
- ignoring stderr
- resolving promises inside process-start callbacks
- returning an output path before the output exists
- converting all errors into a generic "conversion complete" state

Every failure must propagate correctly to the caller/UI.

Errors should contain enough information to diagnose the failed conversion.

Do not expose raw internal stack traces to normal users unless the application's existing design calls for it, but preserve detailed diagnostics in backend logs.

==================================================
15. PATH AND FILESYSTEM SAFETY
==================================================

Test paths containing:

- spaces
- Unicode characters
- parentheses
- brackets
- apostrophes
- multiple dots
- long filenames
- duplicate names

Do not construct shell commands using unsafe string concatenation when a process API with argument arrays is available.

Verify that:

INPUT PATH
and
OUTPUT PATH

are passed safely as individual arguments.

==================================================
16. OUTPUT DIRECTORY
==================================================

Find the single source of truth for the configured output directory.

Verify every conversion implementation uses the correct output directory.

Do not allow one format implementation to silently write somewhere else.

Before conversion:

- ensure output directory exists
- create it when appropriate
- verify it is writable

After conversion:

- verify the expected file exists there
- return the actual path

==================================================
17. TEST INFRASTRUCTURE
==================================================

If the repository already has tests, expand them.

If conversion testing is weak or nonexistent, create an appropriate test suite.

Tests should cover:

1. Single-file conversion.
2. Multiple-file conversion.
3. Every supported source format.
4. Every supported destination format.
5. Representative cross-format conversions.
6. Invalid inputs.
7. Missing files.
8. Missing converter executable.
9. Converter failure.
10. Output-directory failure.
11. Duplicate filenames.
12. Unicode filenames.
13. Concurrent conversions.
14. Partial batch failure.
15. Output validation.

Do not create fake tests that merely mock the conversion process and therefore cannot detect real FFmpeg argument or output problems.

Use integration tests with small real media fixtures wherever practical.

For real FFmpeg tests, keep fixtures small so the test suite remains practical.

==================================================
18. BUILD A CONVERSION TEST MATRIX
==================================================

After understanding the supported formats, create a machine-readable test matrix.

Conceptually:

source_format | destination_format | expected_supported | tested | converter | result

Do this for Audio, Video, Image, and every other conversion category in the project.

Every advertised conversion must have a clear status.

There should be no unknown conversion paths.

If a pair is intentionally unsupported, identify exactly why and make the UI/backend agree about that limitation.

==================================================
19. FRONTEND/BACKEND CONTRACT
==================================================

Audit the interface between the UI and backend.

Verify that:

- the frontend sends the correct source format
- the frontend sends the correct destination format
- backend routing interprets them correctly
- format names are normalized consistently
- capitalization does not cause failures
- aliases are handled consistently
- backend errors reach the frontend
- progress/completion states are accurate
- output paths returned by the backend correspond to real files

Do not assume a backend bug if the actual problem is the frontend request contract, and vice versa.

Trace the complete request.

==================================================
20. STATIC ANALYSIS + RUNTIME ANALYSIS
==================================================

Use BOTH.

Static analysis:
- inspect architecture
- search references
- trace imports
- trace calls
- inspect types
- inspect error paths
- inspect unreachable/dead code
- inspect duplicated conversion logic

Runtime analysis:
- actually execute conversions
- inspect converter commands
- inspect exit codes
- inspect generated files
- inspect media metadata where appropriate
- test asynchronous behavior
- test batch behavior

A code path is not considered verified merely because it looks correct.

==================================================
21. FIX THE ROOT CAUSES
==================================================

After the audit:

1. Identify all root causes.
2. Group related bugs.
3. Implement architectural fixes.
4. Remove obsolete/duplicated logic where safe.
5. Update callers if necessary.
6. Add regression tests for every discovered bug.
7. Run the complete test suite.
8. Run real conversion tests.
9. Re-run the conversion matrix.

Do not stop after the first successful conversion.

==================================================
22. FINAL EXHAUSTIVE VERIFICATION
==================================================

Before declaring the task complete, perform a final audit.

For EVERY advertised conversion path, answer internally:

- Is the route reachable?
- Is the correct function selected?
- Is the correct converter invoked?
- Are the arguments correct?
- Does the process actually execute?
- Is the process awaited?
- Is the exit status checked?
- Is stderr handled?
- Is the output path correct?
- Does the output directory exist?
- Is the output actually created?
- Is the output non-empty?
- Is the output valid?
- Is the correct format/codec actually present?
- Is the result returned only after completion?
- Does batch conversion work?
- Does concurrent conversion work?
- Are failures correctly reported?

Do not claim "all conversions work" unless you have actually verified them or have a clearly documented reason why a particular path cannot be integration-tested in the current environment.

==================================================
23. IMPORTANT: DO NOT HIDE LIMITATIONS
==================================================

If the environment prevents a test from being executed, explicitly document:

- what could not be tested
- why it could not be tested
- what static evidence was used instead
- what remains uncertain

Never substitute "the code looks correct" for a runtime test when runtime testing is possible.

==================================================
24. FINAL REPORT
==================================================

At the end, provide a concise but complete report containing:

A. Root causes discovered
B. Files changed
C. Architectural changes
D. Conversion paths repaired
E. Tests added
F. Tests actually executed
G. Real FFmpeg/other converter tests performed
H. Output-file verification performed
I. Batch-conversion verification
J. Async/concurrency verification
K. Remaining limitations, if any

Most importantly, distinguish:

VERIFIED WORKING

from

STATICALLY VERIFIED BUT NOT EXECUTED

from

NOT TESTABLE IN CURRENT ENVIRONMENT

Do not report an untested conversion as working.

==================================================
FINAL OBJECTIVE
==================================================

The final backend must behave as a general conversion system.

If a user selects one or multiple input files and chooses a supported destination format, the backend should:

1. Detect each input correctly.
2. Route it to the correct conversion implementation.
3. Invoke the appropriate converter correctly.
4. Await the conversion completely.
5. Detect failures.
6. Validate the resulting file.
7. Save it to the configured output directory.
8. Return the actual generated output.
9. Correctly handle multiple files.
10. Correctly handle concurrent asynchronous conversions.
11. Never report success when conversion/output creation actually failed.

Do not optimize for finishing quickly.

Optimize for correctness, completeness, reproducibility, and elimination of the underlying architectural bugs.

Take as much time as necessary and continue until the entire codebase and conversion matrix have been audited, repaired, and verified as far as the available environment allows.