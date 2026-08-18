# HomeStorage

Put your cabinets "into your phone": take a photo of a cabinet, mark the positions of its shelves/bins, and snap photos of the items in each spot. AI auto-recognizes keywords, and later you can search with one phrase to find where anything is.

A fully local, account-free, cloud-free personal item management tool.

## Features

- **Cabinet map**: take a photo of a cabinet, tap to mark spot positions, drag to adjust, edit titles
- **AI item recognition**: automatically recognizes items in photos and generates names & search keywords (Zhipu GLM-4V-Flash, free forever)
- **Voice input**: speak to enter item names and notes instead of typing
- **Search & jump**: search by item name / keyword / cabinet name / category, tap a result to jump straight to the spot and highlight it
- **Batch entry**: select multiple photos at once, each auto-recognized and stored one by one
- **Quantity tracking**: record counts for identical items, shown as a badge in the list
- **Category stats**: filter by living room / bedroom / kitchen…, each category shows total item count
- **Recently added**: horizontal carousel on the home screen for one-tap access
- **Move items**: entered the wrong spot? Move items to another spot in one tap
- **Trash bin**: deleted cabinets go to trash first — restore or permanently delete
- **Backup & restore**: one-tap export of database + all photos as a ZIP to the Downloads folder
- **Dark mode**: follows the system automatically

## Tech Stack

| Module | Choice |
| --- | --- |
| Language / UI | Kotlin + Jetpack Compose (Material 3) |
| Database | Room 2.6 (with multi-version migrations) |
| Image loading | Coil |
| Camera / Picker | CameraX + Photo Picker |
| AI recognition | Zhipu GLM-4V-Flash (OpenAI-compatible API, OkHttp) |
| Voice | System SpeechRecognizer (custom UI) |
| Min SDK | Android 10 (API 29) |

## Requirements

- JDK 17
- Android Studio (Koala or newer)
- Local Gradle 8.13 (or just sync with Android Studio)

## Build

Option 1: Open the project in Android Studio, wait for Gradle sync, then Run.

Option 2: Command line

```bash
gradle assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## AI Recognition Setup (Optional)

The app works without this — AI auto-recognition is simply disabled (you can still type names manually).

1. Register at [Zhipu AI Open Platform](https://open.bigmodel.cn) and create an API Key (GLM-4V-Flash is free forever)
2. Add it to `local.properties` in the project root (already gitignored, never committed):

```properties
zhipuApiKey=YOUR_KEY
```

## Permissions

- **Microphone**: used for voice input (recognition handled by the system speech engine)
- **Camera / Gallery**: for taking or picking cabinet & item photos
- All data (database, photos) stays in the app's private directory — no network tracking, no accounts

## Project Structure

```
app/src/main/java/com/example/homestorage/
├── MainActivity.kt            # Navigation entry
├── data/
│   ├── ai/ImageRecognizer.kt  # Zhipu GLM-4V-Flash image recognition wrapper
│   ├── backup/BackupManager.kt# ZIP backup / restore
│   ├── db/                    # Room entities / DAOs / migrations
│   ├── image/ImageStore.kt    # Image file storage
│   └── repo/HomeRepository.kt # Repository (business logic)
└── ui/
    ├── home/                  # Home: cabinet grid / search / categories / trash
    ├── cabinet/               # Cabinet detail: photo marking / drag spots
    ├── spot/                  # Spot detail: item list / AI entry
    ├── create/                # Create cabinet
    └── common/                # Shared components (voice input / sheets / buttons)
```

## License

[MIT](./LICENSE)

## Feature Ideas

Have a good idea? We'd love to hear it!

If you feel this app is missing something, [open a feature request on Issues](https://github.com/q454578219/home-storage/issues/new?title=Feature%20idea%3A&labels=idea) and describe your use case in one sentence (e.g. "expiry reminders for food items"). I pick valuable, popular ideas and implement them — **your idea could be the next release**.

## Support

If this app helps you, feel free to [buy the author a coffee on 爱发电](https://afdian.com/a/LinZuo) ☕ Your support keeps maintenance and new features (cloud sync, multi-device, etc.) going.