CREATE TABLE rides (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    driver_user_id      BIGINT NOT NULL,
    origin              VARCHAR(200) NOT NULL,
    destination         VARCHAR(200) NOT NULL,
    departure_at        DATETIME2 NOT NULL,
    total_seats         INT NOT NULL,
    available_seats     INT NOT NULL,
    price_per_seat      DECIMAL(10,2),
    notes               VARCHAR(1000),
    status              VARCHAR(20) NOT NULL
);

CREATE INDEX ix_rides_driver_user_id ON rides (driver_user_id);
CREATE INDEX ix_rides_departure_at ON rides (departure_at);
CREATE INDEX ix_rides_status ON rides (status);

CREATE TABLE ride_requests (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    ride_id             BIGINT NOT NULL,
    passenger_user_id   BIGINT NOT NULL,
    seats_requested     INT NOT NULL,
    message             VARCHAR(500),
    status              VARCHAR(20) NOT NULL,
    created_at          DATETIME2 NOT NULL,
    CONSTRAINT fk_ride_requests_ride FOREIGN KEY (ride_id) REFERENCES rides (id)
);

CREATE INDEX ix_ride_requests_ride_id ON ride_requests (ride_id);
CREATE INDEX ix_ride_requests_passenger_user_id ON ride_requests (passenger_user_id);
CREATE INDEX ix_ride_requests_status ON ride_requests (status);
