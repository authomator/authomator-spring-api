# Authomator Spring API

Authomator API server in Java Spring to:

- register new users
- sign in existing users
- recover passwords using email
- update existing passwords

using JSON Web Tokens (JWT):

- supports token refreshing
- keeps access token as small as possible for optimal performance

## Dependencies

- MongoDB
- Java 8 JDK

## Installation

Fork this repository, then clone your fork, and run:

```bash
$ mvn package

# This wil skip running the tests:
$ mvn -Dmaven.test.skip=true package
```

## Starting the server

```bash
# If you build the jar:
$ java -jar target/authomator-api-0.0.1-SNAPSHOT.jar

# Without building the jar:
$ mvn spring-boot:run
```

Your authomator API is now up and running!

## Configuring the server

Edit `src/main/resources/application.properties` and make necessary adjustments.

Or better declare you config using environment variables.

## JWT Tokens

When a user successfully signs in or registers, 3 tokens are returned:

- **accessToken**: can be used to perform application requests on behalf of the user
- **identityToken**: contains details about the user
- **refreshToken**: can be used to request a new set of tokens

### identityToken

Contains all information that Authomator knows about the user.

```javascript
{
    "iss":"issuer",
    "aud":["audience","audience2"],
    "exp":1441717254,
    "iat":1441713654,
    "nbf":1441713594,
    "sub":"5575e89dd9ebb6c28fa5b358",
    "roles":["USER","ADMIN"],
    "email":"testuser@mydomain.tld",
    "emailVerified": false
}
```

### AccessToken

Contains only `sub` and `roles`. Due to its small size, it is recommended to use this token to authenticate requests with your backend.

```javascript
{
    "iss":"issuer",
    "aud":["audience","audience2"],
    "exp":1441717254,
    "iat":1441713654,
    "nbf":1441713594,
    "sub":"5575e89dd9ebb6c28fa5b358",
    "roles":["USER","ADMIN"],
    "ev" : false
}
```

### RefreshToken

```javascript
{
    "iss":"issuer",
    "aud":"issuer#refresh",
    "exp":1441721022,
    "iat":1441713822,
    "nbf":1441713762,
    "sub":"5575e89dd9ebb6c28fa5b358"
}
```

## Common errors


### 400 Bad Request

```javascript
{
	"message": "Invalid http request",
	"code": "HttpMessageNotReadable"
}
```

### 403 Forbidden

```javascript
{
    "message":"Signup is not allowed",
    "code": "SignupDisabled"
}
```

### 422 Unprocessable Entity

```javascript
{
    "message": "Validation failed",
    "code": "ValidationFailed",
    "fieldErrors": [
        {
            "field": "email",
            "message": "Invalid email or password"
            "code": "CredentialsError"
        },
        {
            "field": "password",
            "message": "Invalid email or password"
            "code": "CredentialsError"
        }
    ]
}
```

## API

### POST /register

Register a new user.

#### Request

##### Content-type

`application/json`

##### Parameters

- `email`: string, required, user email address
- `password`: string, required, user password

#### Response

##### 200 OK

User has been successfully registered.

###### Body

The body already conveniently contains the tokens so you don't have to sign in after registration.

```javascript
{
    "identityToken":"...",
    "accessToken":"...",
    "refreshToken":"..."
}
```

##### 403 Forbidden

Registration is not allowed.

###### Body

See common errors.

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.

#### Server configuration

- `io.authomator.api.register.allow`: set to `false` to disable new registrations
- `io.authomator.api.register.default.roles`: specify roles you want to assign to new users.
- `io.authomator.api.verification.email.enabled`: set to `false` to disable email verification mails

For example, to allow registration and assign every new user the role `“USER"`:

```bash
io.authomator.api.signup.allow=true
io.authomator.api.signup.default.roles=USER
```

To assign multiple roles:

```bash
io.authomator.api.signup.default.roles=USER,REVIEWER
```

Or use environment variables:

```bash
$ export IO_AUTHOMATOR_API_SIGNUP_ALLOW=true
$ export IO_AUTHOMATOR_API_SIGNUP_DEFAULT_ROLES=USER,REVIEWER
```


### POST /sign-in

Sign in an existing user.

#### Request

##### Content-type

`application/json`

##### Parameters

- `email`: string, required, user email address
- `password`: string, required, user password

#### Response

##### 200 OK

User has been successfully signed in.

###### Body

```javascript
{
    "identityToken":"...",
    "accessToken":"...",
    "refreshToken":"..."
}
```

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.

### POST /refresh-tokens

Get a new set of tokens.

#### Request

##### Content-type

`application/json`

##### Parameters

- `refreshToken`: string, required, refresh token

#### Response

##### 200 OK

###### Body

New set of tokens:

```javascript
{
    "identityToken":"...",
    "accessToken":"...",
    "refreshToken":"..."
}
```

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.


### POST /forgot-password

Initiate a password recovery procedure.

#### Request

##### Content-type

`application/json`

##### Parameters

- `email`: string, required, user email address
- `url`: string, required, link to specify in the email

#### Response

##### 204 No Content

Successfully sent email to user with password reset link.

The email contains a reset link that points to the `url` of the request
appended with a `reset-token` query string parameter.

The `reset-token` can then be used to reset the password.

##### 403 Forbidden

`url` is not accepted.

###### Body

See common errors.

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.


#### Server configuration

- `io.authomator.api.forgotPassword.allowedDomains`: list of allowed domains for the `url`. The protocol is also check if non-https locations are specified.



### POST /reset-password

Reset a password using a reset token.

#### Request

##### Content-type

`application/json`

##### Parameters

- `resetToken`: string, required, reset token
- `newPassword`: string, required, new password

#### Response

##### 200 Ok

Password successfully reset.

###### Body

New set of tokens so no explicit sign-in is required after reset:

```javascript
{
    "identityToken":"...",
    "accessToken":"...",
    "refreshToken":"..."
}
```

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.


### PUT /password

Update a password using an access token and the old password.

#### Request

##### Content-type

`application/json`

##### Parameters

- `accessToken`: string, required, access token
- `oldPassword`: string, required, old password
- `newPassword`: string, required, new password

#### Response

##### 200 Ok

Password successfully updated.

###### Body

New set of tokens so no explicit sign-in is required after update:

```javascript
{
    "identityToken":"...",
    "accessToken":"...",
    "refreshToken":"..."
}
```

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.

### POST /send-confirm-email

Send a confirm your email address mail.
The confirmation token is sent through email and exposed in the url through the query parameter `confirm-email-token`.
When requesting the confirmation it is ok to add other query parameters in the url, these will remain untouched.
Please note that all restrictions that apply to reset tokens also apply to confirm email (httpsOnly, allowedDomains).


#### Request

##### Content-type

`application/json`

##### Parameters

- `accessToken`: string, required, the sub from the access or identity token
- `url`: string, required, link to specify in the email

#### Response

##### 204 No Content

Email successfully confirmed.

##### 403 Forbidden

Possible reasons:

- The requested url is not secure (NonSecureUrl)
- The requested url is not allowed (UnauthorizedDomain)
- Email confirmation is not enabled (EmailConfirmationNotEnabled)
- Email confirmation was already performed (UserEmailConfirmedAlready)

###### Body

See common errors.

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.


### POST /confirm-email

Confirm an email address using a confirm token. To send the confirm your email mail, use the /send-confirm-email endpoint.
The confirmation token is sent through email and exposed in the url through the query parameter `confirm-email-token`.

An example confirmation url could look like this:

```
http://myapp.authomator.io/path?my-q=someval&confirm-email-token=<token>
```


#### Request

##### Content-type

`application/json`

##### Parameters

- `confirmEmailToken`: string, required, confirmation token

#### Response

##### 204 No Content

Email successfully confirmed.

##### 403 Forbidden

Possible reasons:

- Email confirmation is not enabled (EmailConfirmationNotEnabled)
- Email confirmation was already performed (UserEmailConfirmedAlready)

##### 422 Unprocessable Entity

The submitted data is not valid.

###### Body

See common errors.


## Change log

### v1.0.2

- Add confirm you email address functionality

### v1.0.1

- Fixes some validation issues and incorrect json field in reset-password

### v1.0.0

- Initial production version

## License

MIT
