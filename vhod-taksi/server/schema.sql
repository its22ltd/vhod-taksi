-- MSSQL схема за приложението "Вход Такси"
-- Изпълни веднъж в базата (напр. през SQL Server Management Studio)

IF DB_ID('VhodTaksi') IS NULL
    CREATE DATABASE VhodTaksi;
GO

USE VhodTaksi;
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Payments')
BEGIN
    CREATE TABLE Payments (
        Uuid           NVARCHAR(64)  NOT NULL PRIMARY KEY,
        Device         NVARCHAR(64)  NOT NULL,
        Apt            NVARCHAR(32)  NOT NULL,
        Name           NVARCHAR(200) NULL,
        People         INT           NOT NULL,
        Period         NVARCHAR(16)  NOT NULL,
        Amount         DECIMAL(10,2) NOT NULL,
        Personal       DECIMAL(10,2) NOT NULL,
        ElevatorShare  DECIMAL(10,2) NOT NULL,
        OtherShare     DECIMAL(10,2) NOT NULL,
        Ts             BIGINT        NOT NULL,
        CreatedAt      DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_Payments_Period ON Payments(Period);
    CREATE INDEX IX_Payments_Apt    ON Payments(Apt);
END
GO
