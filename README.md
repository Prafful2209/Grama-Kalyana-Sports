# Grama-Kalyana Sports App

A professional sports scoring and management application designed for village-level tournaments (Grama Kalyana). It supports multiple sports including Cricket, Kabaddi, and Volleyball, providing real-time score updates to fans and an intuitive scoring panel for administrators.

## Features

- **Real-time Live Scores**: Instant updates for fans using Firebase Realtime Database.
- **Admin Scoring Panel**: Dedicated interfaces for scoring Cricket, Kabaddi, and Volleyball.
- **Match Management**: Create, reschedule, and manage matches and tournaments.
- **Team & Player Profiles**: Track player statistics across different sports.
- **Chasing Target Logic**: Automatically calculates runs and balls remaining for Cricket.
- **Kabaddi Professional Timing**: 20-minute match halves and 30-second raid timers.

## 🛠 How to Build and Setup

Follow these steps to get the project running on your local machine:

### 1. Prerequisites
- **Android Studio**: Jellyfish (2023.3.1) or newer recommended.
- **Java**: JDK 17.
- **Firebase Account**: Access to Firebase Console.

### 2. Firebase Configuration (Crucial)
This app relies on Firebase for authentication and database features.
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project named **Grama Sports**.
3. Add an Android app to the project:
   - Package name: `com.grama.sports`
4. **Download `google-services.json`** and place it in the `app/` directory of this project.
5. In the Firebase Console, enable:
   - **Authentication**: Email/Password and Anonymous methods.
   - **Realtime Database**: Create a database in your preferred region.
6. Set the **Realtime Database Rules** to allow read/write for testing (or secure them with Auth rules):
   ```json
   {
     "rules": {
       ".read": true,
       ".write": "auth != null"
     }
   }
   ```

### 3. Build and Run
1. Open the project in Android Studio.
2. Let Gradle synchronize (this may take a few minutes).
3. Connect your Android device or start an emulator.
4. Click **Run 'app'**.

## 📖 Using the App

### Admin Mode
- To access scoring controls, register an account and log in. 
- In the Dashboard, you will see a toggle for "Admin Mode" if you are logged in.
- Admins can create Tournaments, Teams, and Players first, then schedule Matches.
- Once a match is scheduled, click "Start Live" to open the Scorer Panel.

### Scoring Logic
- **Cricket**: Chasing info (e.g., "Needs 20 runs from 12 balls") appears automatically in the 2nd Innings while the match is **Live**.
- **Kabaddi**: 
  - Click **Start Timer** to begin match countdown and the 30s Raid Timer.
  - The Raid Timer resets to 30s whenever a point is scored or the raiding team is switched.
  - Clicking a Team Card switches who is currently raiding.
- **Volleyball**: Professional best-of-5 set tracking with auto-set completion logic.

## Project Structure
- `com.grama.sports.data`: Firebase configuration and repository.
- `com.grama.sports.models`: Shared data models for matches, scores, and players.
- `com.grama.sports.viewmodel`: Main business logic (`AppViewModel.kt`).
- `com.grama.sports.ui`: Jetpack Compose UI screens organized by feature.

## License
MIT License
