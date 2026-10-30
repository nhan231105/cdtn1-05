
-- V2: Seed data for Smart CRM L8
-- Synthetic demo data, not real customer records.
-- Reference tables from L2/L4 only.

-- 1. Service centers
INSERT INTO service_center (center_id, center_name)
VALUES
    (101, 'Trung tam Tan Binh - DEMO'),
    (102, 'Trung tam Thu Duc - DEMO');

-- 2. Customers (fictional test numbers)
INSERT INTO customer (customer_id, phone)
VALUES
    (201, '0900000001'),
    (202, '0900000002');

-- 3. Technicians
INSERT INTO technician (technician_id, center_id)
VALUES
    (301, 101),
    (302, 102);

-- 4. Warranty tickets
INSERT INTO ticket (
    ticket_id,
    ticket_code,
    customer_id,
    center_id,
    technician_id,
    device_id,
    status,
    closed_at
)
VALUES
    (
        401,
        'DEMO-L8-0001',
        201,
        101,
        301,
        501,
        'DA_DONG',
        CURRENT_TIMESTAMP - INTERVAL 1 DAY
    ),
    (
        402,
        'DEMO-L8-0002',
        202,
        101,
        301,
        502,
        'DA_DONG',
        CURRENT_TIMESTAMP - INTERVAL 2 DAY
    ),
    (
        403,
        'DEMO-L8-0003',
        202,
        102,
        302,
        503,
        'DA_DONG',
        CURRENT_TIMESTAMP - INTERVAL 1 DAY
    ),
    (
        404,
        'DEMO-L8-0004',
        201,
        101,
        301,
        504,
        'DANG_XU_LY',
        NULL
    );

-- survey_invitation and survey_response remain empty.
-- Their records will be created through L8 business APIs.
