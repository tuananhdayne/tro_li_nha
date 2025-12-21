# SmartHome API Documentation

## Tổng quan
API này cung cấp các endpoint để quản lý hệ thống SmartHome, bao gồm xác thực JWT, quản lý thiết bị, nhà, phòng và quản trị viên.

## Base URL
```
http://localhost:5149/api
```

## Xác thực
API sử dụng JWT Bearer token để xác thực. Thêm header sau vào request:
```
Authorization: Bearer <your_jwt_token>
```

## Endpoints

### 1. Authentication (`/api/auth`)

#### Đăng nhập
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user-id",
    "email": "user@example.com",
    "displayName": "User Name",
    "roles": ["Member"]
  }
}
```

#### Đăng ký
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

#### Đăng xuất
```http
POST /api/auth/logout
Authorization: Bearer <token>
```

#### Đổi mật khẩu
```http
POST /api/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "oldPassword": "oldpassword",
  "newPassword": "newpassword",
  "confirmPassword": "newpassword"
}
```

#### Lấy thông tin profile
```http
GET /api/auth/profile
Authorization: Bearer <token>
```

#### Cập nhật profile
```http
PUT /api/auth/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "displayName": "New Name",
  "phoneNumber": "0123456789"
}
```

### 2. Devices (`/api/device`)

#### Lấy danh sách thiết bị
```http
GET /api/device?roomId=1&skip=0&take=10
Authorization: Bearer <token>
```

#### Tìm kiếm thiết bị
```http
GET /api/device/search?roomId=1&keyword=light
Authorization: Bearer <token>
```

#### Lấy thông tin thiết bị
```http
GET /api/device/{id}
Authorization: Bearer <token>
```

#### Tạo thiết bị mới
```http
POST /api/device
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Living Room Light",
  "type": "Light",
  "roomID": 1,
  "userID": "user-id"
}
```

#### Cập nhật thiết bị
```http
PUT /api/device/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Light Name",
  "type": "Light",
  "roomID": 1
}
```

#### Xóa thiết bị
```http
DELETE /api/device/{id}
Authorization: Bearer <token>
```

#### Điều khiển thiết bị
```http
POST /api/device/{id}/control
Authorization: Bearer <token>
Content-Type: application/json

{
  "command": "turn_on"
}
```

### 3. Houses (`/api/house`)

#### Lấy danh sách nhà
```http
GET /api/house?skip=0&take=10
Authorization: Bearer <token>
```

#### Tìm kiếm nhà
```http
GET /api/house/search?keyword=home
Authorization: Bearer <token>
```

#### Lấy thông tin nhà
```http
GET /api/house/{id}
Authorization: Bearer <token>
```

#### Tạo nhà mới
```http
POST /api/house
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "My Home",
  "location": "123 Main St"
}
```

#### Cập nhật nhà
```http
PUT /api/house/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Home Name",
  "location": "456 New St"
}
```

#### Xóa nhà
```http
DELETE /api/house/{id}
Authorization: Bearer <token>
```

#### Lấy danh sách thành viên nhà
```http
GET /api/house/{id}/members
Authorization: Bearer <token>
```

#### Thêm thành viên vào nhà
```http
POST /api/house/{id}/members
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "user-id",
  "role": "Member"
}
```

#### Xóa thành viên khỏi nhà
```http
DELETE /api/house/{id}/members/{userId}
Authorization: Bearer <token>
```

#### Tham gia nhà
```http
POST /api/house/join
Authorization: Bearer <token>
Content-Type: application/json

{
  "ownerId": "owner-id",
  "houseId": 1
}
```

### 4. Rooms (`/api/room`)

#### Lấy danh sách phòng
```http
GET /api/room?houseId=1&skip=0&take=10
Authorization: Bearer <token>
```

#### Tìm kiếm phòng
```http
GET /api/room/search?houseId=1&keyword=living
Authorization: Bearer <token>
```

#### Lấy thông tin phòng
```http
GET /api/room/{id}
Authorization: Bearer <token>
```

#### Tạo phòng mới
```http
POST /api/room
Authorization: Bearer <token>
Content-Type: application/json

{
  "houseId": 1,
  "name": "Living Room",
  "detail": "Main living area"
}
```

#### Cập nhật phòng
```http
PUT /api/room/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Room Name",
  "detail": "Updated description"
}
```

#### Xóa phòng
```http
DELETE /api/room/{id}
Authorization: Bearer <token>
```

#### Lấy thiết bị trong phòng
```http
GET /api/room/{id}/devices
Authorization: Bearer <token>
```

#### Thêm thiết bị vào phòng
```http
POST /api/room/{id}/devices/{deviceId}
Authorization: Bearer <token>
```

#### Xóa thiết bị khỏi phòng
```http
DELETE /api/room/{id}/devices/{deviceId}
Authorization: Bearer <token>
```

### 5. Admin (`/api/admin`) - Yêu cầu role Admin

#### Quản lý người dùng
```http
GET /api/admin/users
Authorization: Bearer <token>
```

```http
GET /api/admin/users/{id}
Authorization: Bearer <token>
```

```http
PUT /api/admin/users/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "user@example.com",
  "displayName": "User Name",
  "phoneNumber": "0123456789",
  "role": "Member"
}
```

```http
DELETE /api/admin/users/{id}
Authorization: Bearer <token>
```

#### Quản lý nhà
```http
GET /api/admin/houses
Authorization: Bearer <token>
```

```http
GET /api/admin/houses/{id}
Authorization: Bearer <token>
```

```http
DELETE /api/admin/houses/{id}
Authorization: Bearer <token>
```

#### Quản lý thiết bị
```http
GET /api/admin/devices
Authorization: Bearer <token>
```

```http
GET /api/admin/devices/{id}
Authorization: Bearer <token>
```

```http
DELETE /api/admin/devices/{id}
Authorization: Bearer <token>
```

#### Quản lý vai trò
```http
GET /api/admin/roles
Authorization: Bearer <token>
```

```http
POST /api/admin/roles
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "NewRole"
}
```

```http
DELETE /api/admin/roles/{name}
Authorization: Bearer <token>
```

#### Đăng nhập với tư cách người dùng khác
```http
POST /api/admin/users/{id}/login-as
Authorization: Bearer <token>
```

## Mã lỗi HTTP

- `200 OK` - Thành công
- `201 Created` - Tạo thành công
- `400 Bad Request` - Dữ liệu không hợp lệ
- `401 Unauthorized` - Chưa xác thực hoặc token không hợp lệ
- `403 Forbidden` - Không có quyền truy cập
- `404 Not Found` - Không tìm thấy tài nguyên
- `500 Internal Server Error` - Lỗi server

## Ví dụ sử dụng với JavaScript

```javascript
// Đăng nhập
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
});

const loginData = await loginResponse.json();
const token = loginData.token;

// Sử dụng token để gọi API khác
const devicesResponse = await fetch('/api/device', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const devices = await devicesResponse.json();
```

## Lưu ý

1. Tất cả các request (trừ đăng nhập và đăng ký) đều yêu cầu JWT token
2. Admin endpoints yêu cầu role "Admin"
3. Một số endpoints yêu cầu quyền sở hữu (Owner) của nhà/phòng
4. Token có thời hạn 7 ngày
5. Sử dụng HTTPS trong môi trường production 