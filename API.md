# fnPhoto TV API Documentation

This document describes the API endpoints and authentication mechanism used by fnPhoto TV to communicate with Feiniu NAS photo gallery.

## Authentication Mechanism

### Overview

All API requests require authentication using two headers:
- `accesstoken`: Obtained from WebSocket login
- `authx`: Request signature for replay protection

The authentication mechanism is based on [fnnas-api](https://github.com/FNOSP/fnnas-api) implementation.

### WebSocket Login

To obtain an access token:

```javascript
// Connect to WebSocket
ws://{NAS_URL}:5666/p/api/v1/auth/ws

// Send login request
{
  "msg_id": "1",
  "action": "login",
  "data": {
    "username": "your_username",
    "password": "your_password"
  }
}

// Response
{
  "msg_id": "1",
  "code": 0,
  "data": {
    "token": "access_token_here",
    "secret": "base64_encoded_secret"
  }
}
```

### AuthX Header Generation

The `authx` header is generated for each request using the following algorithm:

#### 1. Generate Nonce

A 6-digit random number:
```java
SecureRandom random = new SecureRandom();
int nonce = 100000 + random.nextInt(900000);
```

#### 2. Get Timestamp

Current time in milliseconds:
```java
String timestamp = String.valueOf(System.currentTimeMillis());
```

#### 3. Process Query Parameters (GET Requests)

For GET requests with query parameters:
- Parse parameters from the query string
- Sort parameters alphabetically by key
- Exclude `null` and `undefined` values
- Reconstruct sorted query string

Example:
```
Input:  "end_time=2026-01-30&limit=10&start_time=2026-01-01"
Output: "end_time=2026-01-30&limit=10&start_time=2026-01-01" (sorted)
```

#### 4. Calculate Payload Hash

For GET requests:
- Without parameters: `md5("")`
- With parameters: `md5(sorted_query_string)`

For POST/PUT requests:
```
md5(request_body)
```

#### 5. Construct Signature String

Format:
```
{API_KEY}_{path}_{nonce}_{timestamp}_{payload_hash}_{API_SECRET}
```

#### 6. Generate Signature

```
sign = md5(signature_string)
```

#### 7. Construct AuthX Header

```
authx = "nonce={nonce}&timestamp={timestamp}&sign={sign}"
```

## API Endpoints

### 1. Get Timeline

Returns photo timeline grouped by date.

**Endpoint:** `GET /p/api/v1/gallery/timeline`

**Headers:**
```
accesstoken: {token}
authx: {generated_authx}
```

**Response:**
```json
{
  "code": 0,
  "msg": "",
  "data": {
    "list": [
      {
        "year": 2026,
        "month": 1,
        "day": 30,
        "itemCount": 5
      }
    ]
  }
}
```

### 2. Get Photos by Date Range

Returns detailed photo information for a specific date range.

**Endpoint:** `GET /p/api/v1/gallery/getList`

**Headers:**
```
accesstoken: {token}
authx: {generated_authx}
```

**Query Parameters:**
- `start_time`: Start datetime (format: `2026:01:30 00:00:00`)
- `end_time`: End datetime (format: `2026:01:30 23:59:59`)
- `limit`: Maximum number of results
- `offset`: Pagination offset
- `mode`: Display mode (`index`)

**Response:**
```json
{
  "code": 0,
  "msg": "",
  "data": {
    "count": null,
    "hasNext": null,
    "list": [
      {
        "id": 31970,
        "fileName": "IMG_20260130_174504.jpg",
        "category": "photo",
        "fileType": "jpeg",
        "additional": {
          "thumbnail": {
            "mUrl": "/p/api/v1/stream/p/t/31970/m/uuid",
            "sUrl": "/p/api/v1/stream/p/t/31970/s/uuid",
            "xsUrl": "/p/api/v1/stream/p/t/31970/xs/uuid",
            "originalUrl": "/p/api/v1/stream/p/t/31970/o/uuid"
          }
        }
      }
    ]
  }
}
```

### 3. Get App Version

Returns photo gallery application version.

**Endpoint:** `GET /p/api/v1/app/version`

**Headers:**
```
accesstoken: {token}
authx: {generated_authx}
```

**Response:**
```json
{
  "errno": 0,
  "result": "succ",
  "data": {
    "version": "1.2.3"
  }
}
```

### 4. Get Photo Statistics

Returns user photo statistics.

**Endpoint:** `GET /p/api/v1/user_photo/stat`

**Headers:**
```
accesstoken: {token}
authx: {generated_authx}
```

**Response:**
```json
{
  "code": 0,
  "msg": "",
  "data": {
    "photoCount": 28085,
    "videoCount": 1322,
    "isAdmin": false
  }
}
```

## Error Handling

### Response Codes

- `code: 0` - Success
- `code: 401` - Authentication failed
- `code: 403` - Permission denied
- `code: 404` - Resource not found
- `code: 500` - Server error

### Authentication Errors

If `authx` validation fails, the server returns:
```json
{
  "code": 401,
  "msg": "Authentication failed"
}
```

Common causes:
- Incorrect API key or secret
- Expired or invalid nonce
- Timestamp too old (replay attack protection)
- Invalid signature

## Security Considerations

1. **API Secret Storage**: The API secret should be securely stored and never exposed to clients
2. **Timestamp Validation**: Server validates timestamp is within acceptable window (usually ±5 minutes)
3. **Nonce Uniqueness**: Each request should use a unique nonce to prevent replay attacks
4. **HTTPS Recommended**: Use HTTPS in production to prevent MITM attacks

## Rate Limiting

The API may implement rate limiting. When exceeded:
```json
{
  "code": 429,
  "msg": "Rate limit exceeded"
}
```

## References

- [fnnas-api](https://github.com/FNOSP/fnnas-api) - Original authentication implementation
- Feiniu NAS Photo Gallery API Documentation
