# ParkTrack - Smart VIP Parking Facility Management System

## Overview
ParkTrack is a comprehensive Android mobile application for managing smart VIP parking facilities. It supports two types of users: Admin (Parking Manager) and Vehicle Owner (Customer).

## Features

### Admin (Parking Manager) Features
- **User Management**: Register and manage vehicle owners
- **Vehicle Management**: Register and manage vehicle details  
- **Parking Lot Management**: Create and manage multiple parking lots with locations
- **Pricing Management**: Define different parking rates (Normal, VIP, Hourly, Overnight)
- **QR Code Scanning**: Scan QR codes for vehicle entry and exit
- **Session Tracking**: Record time-in and time-out for each parking session
- **Automatic Billing**: Calculate parking charges automatically based on duration and rate
- **Reporting**: Generate monthly parking reports
- **Invoice Management**: Generate and track monthly invoices
- **Overdue Management**: Track and manage overdue charges

### Vehicle Owner (Customer) Features
- **Profile Management**: View and edit personal profile details
- **Vehicle Management**: View and manage registered vehicles
- **QR Token Generation**: Generate QR codes for parking access
- **Parking History**: View detailed parking session history (date, time-in, time-out)
- **Charge Management**: View all parking charges and payments status
- **Invoice Viewing**: View and manage monthly invoices
- **Overdue Tracking**: Track overdue charges and payment status
- **Parking Lot Information**: View all parking lots with location and availability

## Technology Stack

### Frontend
- **Kotlin**: Programming language
- **Jetpack Compose**: UI framework for modern Android UI
- **Android Navigation**: Navigation between screens
- **Coil**: Image loading and caching

### Backend
- **Firebase Authentication**: User authentication and management
- **Firebase Firestore**: Real-time NoSQL database
- **Firebase Analytics**: Usage analytics

### Libraries
- **Hilt**: Dependency injection framework
- **Coroutines**: Asynchronous programming
- **Retrofit**: HTTP client (if needed for APIs)
- **ML Kit**: Barcode/QR code scanning
- **CameraX**: Camera integration for QR scanning
- **Google Maps**: Map integration for parking lot locations
- **Material Design 3**: Modern UI components

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/parktrack/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── model/           # Data models
│   │   │   │   ├── repository/      # Firebase repositories
│   │   │   │   └── service/         # Services (if any)
│   │   │   ├── di/                  # Dependency Injection (Hilt)
│   │   │   ├── ui/
│   │   │   │   ├── admin/           # Admin-only screens
│   │   │   │   ├── driver/          # Driver-only screens
│   │   │   │   ├── auth/            # Authentication screens
│   │   │   │   ├── components/      # Reusable composables
│   │   │   │   ├── screens/         # Shared screens
│   │   │   │   ├── theme/           # Theme configuration
│   │   │   │   ├── onboarding/      # Onboarding screens
│   │   │   │   └── navigation/      # Navigation setup
│   │   │   ├── viewmodel/           # ViewModels
│   │   │   ├── utils/               # Utility functions
│   │   │   └── billing/             # Billing utilities
│   │   └── res/
│   │       ├── drawable/
│   │       ├── values/
│   │       └── xml/
│   └── test/
├── build.gradle.kts
└── google-services.json
```

## Data Models

### User
```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val fullName: String,
    val role: UserRole,  // DRIVER or ADMIN
    val phoneNumber: String,
    val vehicleNumber: String,
    val profileImageUrl: String,
    val isVerified: Boolean,
    val createdAt: Long
)
```

### Vehicle
```kotlin
data class Vehicle(
    val id: String,
    val ownerId: String,
    val vehicleNumber: String,
    val vehicleModel: String,
    val vehicleColor: String,
    val vehicleType: String,
    val registrationNumber: String,
    val isActive: Boolean
)
```

### ParkingLot
```kotlin
data class ParkingLot(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val totalSpaces: Int,
    val availableSpaces: Int,
    val occupiedSpaces: Int,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val hasEVCharging: Boolean,
    val hasDisabledParking: Boolean
)
```

### ParkingRate
```kotlin
data class ParkingRate(
    val id: String,
    val parkingLotId: String,
    val rateType: RateType,  // NORMAL, VIP, HOURLY, OVERNIGHT
    val basePricePerHour: Double,
    val maxDailyPrice: Double,
    val minChargeHours: Double,
    val vipMultiplier: Double,  // 1.5 for 50% extra
    val isActive: Boolean
)
```

### ParkingSession
```kotlin
data class ParkingSession(
    val id: String,
    val driverId: String,
    val vehicleNumber: String,
    val entryTime: Timestamp,
    val exitTime: Timestamp?,
    val gateLocation: String,
    val scannedByAdminId: String,
    val status: String,  // ACTIVE or COMPLETED
    val durationMinutes: Long
)
```

### ParkingCharge
```kotlin
data class ParkingCharge(
    val id: String,
    val sessionId: String,
    val driverId: String,
    val vehicleNumber: String,
    val parkingLotId: String,
    val entryTime: Timestamp,
    val exitTime: Timestamp,
    val durationMinutes: Long,
    val rateType: String,
    val baseRate: Double,
    val calculatedCharge: Double,
    val discountApplied: Double,
    val finalCharge: Double,
    val isPaid: Boolean,
    val paymentMethod: String,
    val isOverdue: Boolean,
    val overdueCharge: Double
)
```

### Invoice
```kotlin
data class Invoice(
    val id: String,
    val driverId: String,
    val month: String,  // YYYY-MM format
    val totalSessions: Int,
    val totalCharges: Double,
    val totalDiscount: Double,
    val totalOverdueCharges: Double,
    val netAmount: Double,
    val amountPaid: Double,
    val balanceDue: Double,
    val isPaid: Boolean,
    val paymentStatus: String,  // PENDING, PAID, OVERDUE
    val charges: List<String>  // List of charge IDs
)
```

### ParkingReport
```kotlin
data class ParkingReport(
    val id: String,
    val reportType: ReportType,  // MONTHLY, QUARTERLY, ANNUAL
    val periodStart: Timestamp,
    val periodEnd: Timestamp,
    val totalSessions: Int,
    val totalRevenue: Double,
    val amountCollected: Double,
    val outstandingAmount: Double,
    val averageSessionDuration: Long,
    val numberOfUniqueVehicles: Int,
    val paidSessions: Int,
    val unpaidSessions: Int,
    val overdueSessions: Int
)
```

## Firebase Database Structure

```
users/
├── {userId}/
│   ├── id
│   ├── email
│   ├── fullName
│   ├── role
│   ├── phoneNumber
│   └── createdAt

vehicles/
├── {vehicleId}/
│   ├── id
│   ├── ownerId
│   ├── vehicleNumber
│   ├── vehicleModel
│   ├── vehicleColor
│   └── isActive

parkingLots/
├── {lotId}/
│   ├── id
│   ├── name
│   ├── latitude
│   ├── longitude
│   ├── totalSpaces
│   ├── availableSpaces
│   └── address

parkingRates/
├── {rateId}/
│   ├── id
│   ├── parkingLotId
│   ├── rateType
│   ├── basePricePerHour
│   ├── maxDailyPrice
│   └── isActive

parkingSessions/
├── {sessionId}/
│   ├── id
│   ├── driverId
│   ├── vehicleNumber
│   ├── entryTime
│   ├── exitTime
│   ├── gateLocation
│   ├── status
│   └── durationMinutes

parkingCharges/
├── {chargeId}/
│   ├── id
│   ├── driverId
│   ├── sessionId
│   ├── calculatedCharge
│   ├── finalCharge
│   ├── isPaid
│   └── createdAt

invoices/
├── {invoiceId}/
│   ├── id
│   ├── driverId
│   ├── month
│   ├── totalCharges
│   ├── netAmount
│   ├── balanceDue
│   └── paymentStatus

parkingReports/
├── {reportId}/
│   ├── id
│   ├── reportType
│   ├── periodStart
│   ├── totalSessions
│   ├── totalRevenue
│   ├── amountCollected
│   └── outstandingAmount
```

## Key Features Implementation

### 1. QR Code Generation and Scanning
- **Generation**: Users generate time-limited QR codes for entry/exit
- **Scanning**: Admins scan QR codes at gates to record entry/exit
- **Validation**: QR codes are validated for authenticity and expiration
- **Security**: Hash-based validation to prevent tampering

### 2. Real-time Parking Tracking
- **Active Sessions**: Display current parking sessions in real-time
- **Duration Tracking**: Live countdown of parking duration
- **Status Updates**: Real-time updates of space availability

### 3. Automatic Billing
- **Rate Calculation**: Based on duration, rate type, and parking lot
- **Charge Generation**: Automatic charge creation on session exit
- **Invoice Generation**: Monthly invoices created from charges
- **Payment Tracking**: Track paid, unpaid, and overdue charges

### 4. Google Maps Integration
- **Parking Lot Locations**: Display parking lots on map
- **Nearby Lots**: Find parking lots near user's location
- **Distance Calculation**: Haversine formula for accurate distances
- **Lot Details**: View lot information, availability, and features

### 5. Reports and Analytics
- **Monthly Reports**: Comprehensive parking and revenue reports
- **Session Statistics**: Track sessions, duration, and charges
- **Revenue Analytics**: Total revenue, collected amount, outstanding
- **Occupancy Data**: Peak hours, average occupancy percentage

## Setup Instructions

### Prerequisites
- Android Studio Latest
- Java 11 or higher
- Android SDK 24+
- Google Play Services

### Firebase Setup
1. Create a Firebase project on [Firebase Console](https://console.firebase.google.com)
2. Add Android app to the project
3. Download `google-services.json` and place in `app/` directory
4. Enable the following Firebase services:
   - Authentication (Email/Password)
   - Firestore Database
   - Analytics

### API Keys
1. Get Google Maps API key from [Google Cloud Console](https://console.cloud.google.com)
2. Add the key to `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```

### Running the App
```bash
# Clone the repository
git clone <repo-url>

# Open in Android Studio
android-studio d:\projects\Kotlin\ParkTrack--MobileCW

# Build and run on emulator or device
```

## Usage Guide

### For Admin
1. **Login**: Use admin credentials
2. **Manage Lots**: Add/edit parking lots
3. **Set Rates**: Configure pricing for each lot
4. **Scan QR Codes**: Record vehicle entry/exit
5. **View Dashboard**: Monitor active sessions
6. **Generate Reports**: Create monthly reports
7. **Manage Invoices**: Track payments

### For Driver
1. **Login**: Use driver credentials
2. **Register Vehicles**: Add vehicle details
3. **Generate QR Code**: Create entry/exit QR codes
4. **View History**: Check parking session history
5. **View Invoices**: Review monthly bills
6. **Manage Payment**: Pay outstanding charges
7. **View Nearby Lots**: Find available parking

## API Endpoints (if using backend)

All data is stored in Firebase Firestore. No additional backend API is required.

## Error Handling
- Network errors with retry mechanisms
- Permission handling for camera and location
- Graceful error dialogs for user feedback
- Comprehensive logging for debugging

## Security Considerations
- Firebase security rules restrict unauthorized access
- QR code validation prevents tampering
- User role-based access control
- Secure password requirements
- Session timeout handling

## Performance Optimization
- Pagination for large datasets
- Caching with Coil for images
- Lazy loading of Firestore data
- Optimized Compose rendering
- Database indexes for common queries

## Testing
- Unit tests for ViewModels
- Integration tests for repositories
- UI tests with Compose testing library
- Manual testing on real devices

## Troubleshooting

### Build Errors
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# If JAVA_HOME is not set
# Windows: set JAVA_HOME=C:\Program Files\Java\jdk-11.0.X
# Linux/Mac: export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.X.jdk/Contents/Home
```

### Firebase Connection Issues
1. Check `google-services.json` is correctly placed
2. Verify Firebase project ID matches
3. Check internet connection
4. Clear app cache and rebuild

### QR Code Issues
1. Ensure camera permissions are granted
2. Check lighting conditions for scanning
3. Verify QR code is not expired
4. Test with different QR code generators

## Future Enhancements
- [ ] Payment gateway integration
- [ ] SMS/Email notifications
- [ ] Push notifications
- [ ] Advanced analytics dashboard
- [ ] Vehicle tracking with GPS
- [ ] Multi-language support
- [ ] Offline mode with sync
- [ ] Machine learning for peak hour prediction
- [ ] License plate recognition
- [ ] Mobile wallet integration

## Contributing
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License
This project is licensed under the MIT License - see LICENSE file for details

## Support
For issues and questions, contact the development team.

## Changelog
- v1.0.0 - Initial release with core features

---

**Built with ❤️ using Kotlin and Firebase**
