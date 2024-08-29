ALTER TABLE _user ADD COLUMN is_deleted boolean DEFAULT FALSE;
ALTER TABLE comment ADD COLUMN is_deleted boolean DEFAULT FALSE;
ALTER TABLE post ADD COLUMN is_deleted boolean DEFAULT FALSE;