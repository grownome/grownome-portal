-- :name create-device! :! :n
-- :doc creates a new device record
INSERT INTO
  devices(id,name,resin_name,short_link,created_on)
VALUES(:id, :name, :resin_name, :short_link, :created_on)

-- :name get-devices :? :n
-- :doc gets all devices
SELECT * FROM devices

-- :name get-device :? :1
-- :doc gets an existing device record
SELECT * FROM devices
WHERE id = :id

-- :name get-images-by-device :? :n
-- :doc  gets all of the images for a device
SELECT * FROM images
WHERE device_id = :id

-- :name get-images-by-device-limit :? :n
-- :doc  gets all of the images for a device to a limit
SELECT * FROM images
WHERE device_id = :id
ORDER BY created_on DESC
LIMIT :limit


-- :name get-timelapses-by-device :? :n
-- :doc  gets all of the images for a device
SELECT * FROM timelapses
 WHERE device_id = :id

-- :name get-timelapses-by-device-limit :? :n
-- :doc  gets all of the images for a device to a limit
SELECT * FROM timelapses
 WHERE device_id = :id
 ORDER BY created_on DESC
 LIMIT :limit


-- :name add-owner :! :n
-- :doc  adds an owner of a device
INSERT INTO
  owner(user_id,device_id,created_on)
VALUES(:user_id,:device_id,:created_on)

-- :name get-user-devices :? :n
-- :doc  gets all of the devices a user own
SELECT * FROM owner
WHERE user_id = :user_id

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id
