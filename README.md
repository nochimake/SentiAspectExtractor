# SentiAspectExtractor
SentiAspectExtractor is an independent project that can extract aspects from given text and a list of text opinions using several syntax-based rules. It is also the stage three of SESAME.

The detailed introduction for the aspect extraction rules can be found in SentiAspectExtractor Introduction.docx


Project structure is as follows:
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── dataAnalysis/
│   │   │   │   └── ...  (Preliminary research-related code)
│   │   │   ├── extractor/ (Main code package)
│   │   │   │   ├── AnalysisOptions
│   │   │   │       (This class represents the analysis options for text extraction, including language settings, preprocessing, rules, and output formats.)
│   │   │   │   ├── Aspect
│   │   │   │   ├── FileIOs
│   │   │   │       (A utility class for reading and writing files)
│   │   │   │   ├── Opinion
│   │   │   │   ├── SentiAspectExtractor
│   │   │   │       (The main class of the project)
│   │   │   │   ├── TestCoreNLP
│   │   │   │       (Test the analysis results of CoreNLP in Chinese and English)
│   │   │   │   ├── coreNLPRules/
│   │   │   │       (Perform the adaptation of Chinese and English NLP rules)
│   │   │   │   ├── textExtractor/
│   │   │   │       (Extracting aspects for text using syntactic rules)
│   │   │   │   ├── textParser/
│   │   │   │       (Parse text using CoreNLP)
│   │   │   │   ├── wordList/
│   │   │   │       (Manage various word lists)
│   │   │   ├── resources/
│   │   │   └── ...


