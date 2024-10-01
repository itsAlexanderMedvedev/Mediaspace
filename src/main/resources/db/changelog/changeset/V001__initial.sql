CREATE TABLE media (
    id               bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    url              varchar(4096) NOT NULL
);

CREATE TABLE _user (
    id                 bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    profile_picture_id bigint,
    username           varchar(20) NOT NULL UNIQUE,
    password           varchar(64) NOT NULL,
    created_at         timestamp   DEFAULT current_timestamp,
    updated_at         timestamp   DEFAULT current_timestamp,

    CONSTRAINT fk_user_has_profile_picture FOREIGN KEY (profile_picture_id) REFERENCES media
);

CREATE TABLE follow (
    follower_id bigint NOT NULL,
    followee_id bigint NOT NULL,
    created_at  timestamp DEFAULT current_timestamp,

    CONSTRAINT pk_follow PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES _user,
    CONSTRAINT fk_followee FOREIGN KEY (followee_id) REFERENCES _user
);

CREATE TABLE post (
    id          bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    _user_id    bigint      NOT NULL,
    title       varchar(64) NOT NULL,
    description varchar(2048),
    created_at  timestamp   DEFAULT current_timestamp,
    updated_at  timestamp   DEFAULT current_timestamp,
    
    CONSTRAINT fk_post_created_by_user FOREIGN KEY (_user_id) REFERENCES _user
);

CREATE TABLE _like (
    _user_id   bigint    NOT NULL,
    post_id    bigint    NOT NULL,

    created_at timestamp DEFAULT current_timestamp,
    
    CONSTRAINT pk_like PRIMARY KEY (_user_id, post_id),
    CONSTRAINT fk_like_user FOREIGN KEY (_user_id) REFERENCES _user,
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES post
);

CREATE TABLE comment (
    id                bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    _user_id          bigint    NOT NULL,
    post_id           bigint    NOT NULL,
    parent_comment_id bigint,
    body              varchar(2048) NOT NULL,
    created_at        timestamp DEFAULT current_timestamp,
    updated_at        timestamp DEFAULT current_timestamp,
    
    CONSTRAINT fk_user_commented_on FOREIGN KEY (_user_id) REFERENCES _user,
    CONSTRAINT fk_comment_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES comment,
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post
);

CREATE TABLE user_tag (
    post_id  bigint NOT NULL,
    _user_id bigint NOT NULL,
    
    CONSTRAINT pk_user_tag PRIMARY KEY (_user_id, post_id),
    CONSTRAINT fk_user_tag_user FOREIGN KEY (_user_id) REFERENCES _user,
    CONSTRAINT fk_user_tag_post FOREIGN KEY (post_id) REFERENCES post
);

CREATE TABLE story (
    id         bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    _user_id   bigint    NOT NULL,
    media_id   bigint    NOT NULL,
    created_at timestamp DEFAULT current_timestamp,
    expires_at timestamp DEFAULT current_timestamp + INTERVAL '24 hours',

    CONSTRAINT fk_story_created_by_user FOREIGN KEY (_user_id) REFERENCES _user,
    CONSTRAINT fk_story_has_media FOREIGN KEY (media_id) REFERENCES media
);

CREATE TABLE post_media (
    post_id  bigint  NOT NULL,
    media_id bigint  NOT NULL,
    position integer NOT NULL,
    CONSTRAINT pk_media_position PRIMARY KEY (media_id, position),
    CONSTRAINT fk_media_associated_with_post FOREIGN KEY (media_id) REFERENCES media,
    CONSTRAINT fk_post_has_media FOREIGN KEY (post_id) REFERENCES post
);
