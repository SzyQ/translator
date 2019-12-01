# ML app translator #AndroidDevChallenge
Translate your Android app automatically with few lines of code and conquer the world!
# Integration
Setup Firebase https://firebase.google.com/docs/android/setup and run the sample.

# Setup
- Initialise Translator in your Application onCreate
```kotlin
override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Translator.init(
            app = this,
            defaultAppLocale = Locale.ENGLISH,
            supportedLocales = listOf(Locale("pl")),
            factory = object : ParserFactory {
                override fun get(view: View): ViewTranslationParser? {
                    return null
                }
            },
            clazz = R.string::class.java
        )
        ...
    }
```
- Add this to your activity
```kotlin
override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(TranslatedContext.wrap(newBase))
}


override fun onCreate(savedInstanceState: Bundle?) {
    TranslationInflaterFactory.setup(this)
    super.onCreate(savedInstanceState)
    ...
}
```
See sample for reference

<img src = "https://j.gifs.com/gZD6R6.gif" width = "320"/>
