-- :name create-device! :! :n
-- :doc creates a new device record
INSERT INTO
  devices(id,name,resin_name,short_link,created_on)
VALUES(:id, :name, :resin_name, :short_link, :created_on)

-- :name get-devices :? :*
-- :doc gets all devices
SELECT * FROM devices

-- :name get-device :? :1
-- :doc gets an existing device record
SELECT * FROM devices
WHERE id = :id

-- :name get-devices-by-user :? :*
-- :doc gets all devices
SELECT d.id, d.name, d.resin_name, d.short_link, d.created_on
  FROM devices d
   INNER JOIN owners o on d.id = o.device_id
         INNER JOIN users u on o.user_id = u.id
WHERE u.id = :id

-- :name get-metrics-by-device :? :*
-- :doc  gets all of the metrics for a device
SELECT * FROM metrics
 WHERE device_id = :id

-- :name get-images-by-device :? :*
-- :doc  gets all of the images for a device
SELECT * FROM images
WHERE device_id = :id

-- :name get-images-by-device-limit :? :*
-- :doc  gets all of the images for a device to a limit
SELECT * FROM images
WHERE device_id = :id
ORDER BY created_on DESC
LIMIT :limit


-- :name get-timelapses-by-device :? :*
-- :doc  gets all of the images for a device
SELECT * FROM timelapses
 WHERE device_id = :id

-- :name get-timelapses-by-device-limit :? :*
-- :doc  gets all of the images for a device to a limit
SELECT * FROM timelapses
 WHERE device_id = :id
 ORDER BY created_on DESC
 LIMIT :limit


-- :name add-owner! :! :n
-- :doc  adds an owner of a device
INSERT INTO
  owners(user_id,device_id,created_on)
VALUES(:user_id,:device_id,:created_on)

-- :name get-user-devices :? :*
-- :doc  gets all of the devices a user own
SELECT * FROM owners
WHERE user_id = :user_id

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, email)
VALUES (:id, :email)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name update-user-is-admin! :! :1
UPDATE users
SET admin = :admin
WHERE id = :id

-- :name update-user-last-used! :! :1
UPDATE users
SET last_login = :last_login, is_active = :is_active
WHERE id = :id




-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-user-by-email :? :1
-- :doc retrieves a user given an email
SELECT * FROM users
WHERE email = :email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id
