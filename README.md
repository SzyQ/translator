# ML app translator #AndroidDevChallenge
Translate your Android app automatically with few lines of code and conquer the world!
# Integration
Setup Firebase https://firebase.google.com/docs/android/setup and run the sample.

# Setup
- Initialise Translator in your Application onCreate
```kotlin
Translator.init(
            app = this,
            defaultAppLocale = Locale.ENGLISH,
            supportedLocales = listOf(Locale("pl")), //Locales which you support apart from your default one
            factory = null,// Parser factories for your custom views
            clazz = R.string::class.java //Add this if you want to prefetch all strings in the app
        )
```
- Add This single line before super.onCreate in your Activity
```kotlin
TranslationInflaterFactory.setup(this)
```
See sample for reference
![](https://j.gifs.com/gZD6R6.gif)
