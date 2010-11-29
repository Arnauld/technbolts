package org.technbolts.couchdb

object HttpCode {
  /**
   * Request completed successfully.
   */
  val OK = 200

  /**
   * Document created successfully.
   */
  val Created = 201

  /**
   * Request has been accepted, but the corresponding operation may not have completed. This is used for background operations, such as database compaction.
   */
  val Accepted = 202


  /**
   * The additional content requested has not been modified. This is used with the ETag system to identify the version of information returned.
   */

  val NotModified = 304

  /**
   * Bad request structure. The error can indicate an error with the request URL, path or headers. Differences in the supplied MD5 hash and content also trigger this error, as this may indicate message corruption.

   */
  val BadRequest = 400

  /**
   * The item requested was not available using the supplied authorization, or authorization was not supplied.
   */

  val Unauthorized = 401

  /**
   * The requested item or operation is forbidden.
   */
  val Forbidden = 403


  /**
   * The requested content could not be found. The content will include further information, as a JSON object, if available. The structure will contain two keys, error and reason. For example:
   * { "error":"not_found","reason":"no_db_file" }
   */
  val NotFound = 404


  /**
   * A request was made using an invalid HTTP request type for the URL requested. For example, you have requested a PUT when a POST is required. Errors of this type can also triggered by invalid URL strings.
   */

  val ResourceNotAllowed = 405


  /**
   * The requested content type is not supported by the server.
   */
  val NotAcceptable = 406


  /**
   * Request resulted in an update conflict.
   */
  val Conflict = 409


  /**
   * The request headers from the client and the capabilities of the server do not match.
   */
  val PreconditionFailed = 412

  /**
   * The content types supported, and the content type of the information being requested or submitted indicate that the content type is not supported.
   */
  val BadContentType = 415


  /**
   * The range specified in the request header cannot be satisfied by the server.
   */
  val RequestedRangeNotSatisfiable = 416


  /**
   * When sending documents in bulk, the bulk load operation failed.
   */
  val ExpectationFailed = 417


  /**
   * The request was invalid, either because the supplied JSON was invalid, or invalid information was supplied as part of the request.
   */
  val InternalServerError = 500


}