Authomator-api
==============

This is a spring based authentication server. The goal is to take care of
authentication/registration/password resets using a REST API.

When a user is registered or logged in, 3 tokens are returned which allows the
application to verify the identity:

-   An identity token that describes the identity of the user

-   An access token, which is reduced in size to only include the `sub` field
    (jwt subject) that uniquely identifies the user

-   A refresh token which can be used to request a new set of tokens without
    requiring the user to re-authenticate.

Dependencies
------------

-   mongodb

-   java 8 JDK

 

Building a standalone jar
-------------------------

Fork this repository, then clone your fork, and run this in your newly created
directory:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
mvn package

# This wil skip running the tests:

mvn -Dmaven.test.skip=true package
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

Launch the server
-----------------

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# If you build the jar:

java -jar target/authomator-api-0.0.1-SNAPSHOT.jar

# Without building the jar:

mvn spring-boot:run
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Your authomator API is now up and running! However, it will use defaults that
will probably not be useful.. so read on how to configure it to do some
authomation for you...

 

Configuration
-------------

Edit src/main/resources/application.properties and make necessary adjustments.
Or better declare you config using environment variables.

 

JWT Tokens contents
===================

 

### IdentityToken contents:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "iss":"issuer",
    "aud":["audience","audience2"],
    "exp":1441717254,
    "iat":1441713654,
    "nbf":1441713594,
    "sub":"5575e89dd9ebb6c28fa5b358",
    "roles":["USER","ADMIN"],
    "email":"testuser@mydomain.tld"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### AccessToken contents:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "iss":"issuer",
    "aud":["audience","audience2"],
    "exp":1441717254,
    "iat":1441713654,
    "nbf":1441713594,
    "sub":"5575e89dd9ebb6c28fa5b358",
    "roles":["USER","ADMIN"]
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### RefreshToken contents:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "iss":"issuer",
    "aud":"issuer#refresh",
    "exp":1441721022,
    "iat":1441713822,
    "nbf":1441713762,
    "sub":"5575e89dd9ebb6c28fa5b358"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

To consume less bandwith it is advisable to use the accessToken for
authenticating requests to a backend. This token only contains the `sub` and
`roles` in order to lower the size of each request.

However the identityToken is usefull as it contains all info about the user, so
that a profile can be build in some backend or used in the frontend. In essence
it is not the goal of authomator to store a full user profile with data such as
address, phone number. This should be up to the application, but data we already
collect will be in the Identity token so that a user does not have to enter it
twice..

 

Using the service
=================

 

Common errors
-------------

 

### Response Data (403 Forbidden)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "message":"Signup is not allowed",
    "code": "SignupDisabled"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data (422 Unprocessable Entity)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "message": "Validation failed",
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

 

Signing up users (POST /api/auth/signup)
----------------------------------------

 

**Make sure you set **`io.authomator.api.signup.allow`** to **`true`** to enable
user signups.**  
By default users that are signed up to the service will not be granted any role.
This can be configured with

`io.authomator.api.signup.default.roles`. in
`src/main/resources/application.properties`

 

For example to allow registration and assign every new user the role `“USER"`

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
io.authomator.api.signup.allow=true
io.authomator.api.signup.default.roles=USER
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

To assign multiple roles:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
io.authomator.api.signup.default.roles=USER,REVIEWER
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

Or instead of changing the config file use environment vars

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
export IO_AUTHOMATOR_API_SIGNUP_ALLOW=true
export IO_AUTHOMATOR_API_SIGNUP_DEFAULT_ROLES=USER,REVIEWER
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Parameters (json body)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "email":      The email of the user to register
    "password":   The password for the new user
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Codes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
200 Ok                       User signup successful
403 Forbidden                Signup is not allowed (config see above)
422 Unprocessable Entity     Wrong parameters or user exists
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data (200 OK)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "it":"it.token.data",
    "at":"at.token.data",
    "rt":"rt.token.data"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

For more information about the JWT tokens see above for the info about the
tokens.

See the section Common Errors for the format of the errors.

 

Logging in users (POST /api/auth/login)
---------------------------------------

 

### Parameters (json body)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "email":      The email of the user to login
    "password":   The password of the user
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Codes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
200 Ok                       User login was successful
422 Unprocessable Entity     Wrong parameters / Invalid credentials
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
        "it":"it.token.data",
        "at":"at.token.data",
        "rt":"rt.token.data"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

For more information about the JWT tokens see above for the info about the
tokens.

 

Refreshing tokens (POST /api/refresh/:refreshtoken)
---------------------------------------------------

 

### Parameters (json body)

None (refreshToken is given in url). Where `<refreshToken>` is the Refresh token
you got when the user logged in or registered. If the token is valid the server
will give you 3 new tokens in a json.

 

### Response Codes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
200 Ok                       User login was successful
422 Bad Request              Invalid refresh token or invalid user
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
        "it":"it.token.data",
        "at":"at.token.data",
        "rt":"rt.token.data"
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For more information about the JWT tokens see above for the info about the
tokens.

For information on the error format see the section above on error formats

 

 

 

 

 

 

 

 

 

 

 

Send password forgotten mail (POST /api/auth/forgot/mail)
----------------------------------------------------

 

### Parameters (json body)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "email":      The email of the user to register
    "url":        The link that needs to be used in the email
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

The url specified in the parameters is checked if it allowed (see
`io.authomator.api.mail.allowedDomains`). The protocol is also checked if
non-https locations are allowed (see `io.authomator.api.mail.allowedDomains`). The
url wil have a query parameter added with the reset token, the name of this
parameter is `reset`. For example this would be the url sent in the email:


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
https://url.as.specied/with/paths?andOriginalparam=OK&reset=<reset token>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Codes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
204 No Content               Mail was sent correctly
400 Bad Request              Email is not in use
403 Forbidden                Url is not acceptable (see body for more info)
422 Unprocessable Entity     Wrong parameters (see body for error info)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data

None

 

 

Change user password (POST /api/auth/reset/:resetToken)
-------------------------------------------------------

### Parameters (json body)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
{
    "password":              The new password to set for the user
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Codes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
204 No Content               Password was reset correctly
400 Bad Request              Invalid reset token
404 Resource Not Found       User does not exist
422 Unprocessable Entity     Wrong parameters (see body for error info)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 

### Response Data

None

 

 

 
