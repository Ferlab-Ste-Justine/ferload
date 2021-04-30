# ferload

This service is used to dowload files stored in an object store s3 compliant. It will :
- Verify if user has access to the object (validating jwt token)
- Generate a presigned url for this object

