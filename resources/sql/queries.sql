-- :name create-device! :! :n
-- :doc creates a new device record
INSERT INTO
  devices(id,name,resin_name,short_link,created_on)
VALUES(:id, :name, :resin_name, :short_link, :created_on);

-- :name get-devices :? :*
-- :doc gets all devices
SELECT * FROM devices;

-- :name get-device :? :1
-- :doc gets an existing device record
SELECT * FROM devices
WHERE id = :id;

-- :name get-devices-by-user :? :*
-- :doc gets all devices
SELECT d.id, d.name, d.resin_name, d.short_link, d.created_on
  FROM devices d
         INNER JOIN owners o on d.id = o.device_id
         INNER JOIN users u on o.user_id = u.id
 WHERE u.id = :id;

-- :name get-metrics-by-device :? :*
-- :doc  gets all of the metrics for a device
SELECT * FROM metrics
 WHERE device_id = :id
 ORDER BY timestamp DESC
 LIMIT 5000;


-- :name get-images-by-device :? :*
-- :doc  gets all of the images for a device
SELECT * FROM images
WHERE device_id = :id;

-- :name get-images-by-device-limit :? :*
-- :doc  gets all of the images for a device to a limit
SELECT * FROM images
 WHERE device_id = :id
 ORDER BY created_on DESC
 LIMIT :limit;


-- :name get-timelapses-by-device :? :*
-- :doc  gets all of the images for a device
SELECT * FROM timelapses
 WHERE device_id = :id;

-- :name get-timelapses-by-device-limit :? :*
-- :doc  gets all of the images for a device to a limit
SELECT * FROM timelapses
 WHERE device_id = :id
 ORDER BY created_on DESC
 LIMIT :limit;


-- :name add-owner! :! :n
-- :doc  adds an owner of a device
INSERT INTO
  owners(user_id,device_id,created_on)
VALUES(:user_id,:device_id,:created_on);

-- :name get-user-devices :? :*
-- :doc  gets all of the devices a user own
SELECT * FROM owners
 WHERE user_id = :user_id;

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
            (id, email)
VALUES (:id, :email);

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
   SET first_name = :first_name, last_name = :last_name, email = :email
 WHERE id = :id;

-- :name update-user-is-admin! :! :1
UPDATE users
   SET admin = :admin
 WHERE id = :id;

-- :name update-user-last-used! :! :1
UPDATE users
   SET last_login = :last_login, is_active = :is_active
 WHERE id = :id;

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
 WHERE id = :id;

-- :name get-user-by-email :? :1
-- :doc retrieves a user given an email
SELECT * FROM users
 WHERE email = :email;

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
 WHERE id = :id;

-- :name create-alert! :! :1
-- :doc creates an alert
INSERT INTO alerts
            (device_id,
            created_on,
            user_id,
            metric_name,
            threshold,
            above,
            below,
            phone_number,
            description)
VALUES (:device_id,
        :created_on,
        :user_id,
        :metric_name,
        :threshold,
        :above,
        :below,
        :phone_number,
        :description);

-- :name delete-alert! :! :1
-- :doc deletes an alert by id
DELETE FROM alerts
 WHERE id = :id;

-- :name get-alerts-by-user :? :n
-- :doc given a user gets all alerts
SELECT *
  FROM alerts
 WHERE user_id = :user_id;

-- :name get-alerts-by-device-and-user :? :n
-- :doc given a user-id and device-id gets all alerts
SELECT *
  FROM alerts
 WHERE device_id = :device_id, user_id = :user_id;

-- :name get-alerts-by-device :? :n
-- :doc given a device-id gets all alerts
SELECT *
  FROM alerts
 WHERE device_id = :device_id;

-- :name get-alert
-- :doc get an alert by alert id
SELECT *
  FROM alerts
 WHERE id = :id;

-- :name get-all-alerts
-- :doc admin method to get all allerts
SELECT *
  FROM alerts;

-- :name update-alert! :! :1
-- :doc updates an alert
UPDATE alerts
   SET
            threshold = :threshold,
            above = :above,
            below = :below,
            phone_number = :phone_number,
            description = :description
 WHERE
   id = :id;

-- :name update-alert-user! :! :1
-- :doc updates an alert validating that the owner of the alert did so
UPDATE alerts
   SET
            threshold = :threshold,
            above = :above,
            below = :below,
            phone_number = :phone_number,
            description = :description
 WHERE
   id = :id AND user_id = :user_id;
