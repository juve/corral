CREATE TABLE site ( 
	id				BIGINT NOT NULL AUTO_INCREMENT,
	name			VARCHAR(255) NOT NULL,
	installPath 	TEXT NOT NULL,
	localPath		TEXT NOT NULL,
	submitPath		TEXT NOT NULL,
	condorPackage	VARCHAR(1024),
	condorVersion	VARCHAR(16),
	status			VARCHAR(16),
	statusMessage	VARCHAR(1024),
	CONSTRAINT		pk_site PRIMARY KEY (id)
) type=InnoDB;

CREATE TABLE site_environment (
	site			BIGINT NOT NULL,
	variable		VARCHAR(255) NOT NULL,
	value			TEXT,
	CONSTRAINT		pk_site_environment PRIMARY KEY (site,variable),
	CONSTRAINT		fk_site_environment_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

CREATE TABLE execution_service (
	site			BIGINT NOT NULL,
	function		ENUM('STAGING','GLIDEIN') NOT NULL,
	serviceContact	VARCHAR(1024),
	serviceType		ENUM('GT2','GT4') NOT NULL,
	project			VARCHAR(255),
	queue			VARCHAR(255),
	proxy			TEXT NOT NULL,
	CONSTRAINT		pk_execution_service PRIMARY KEY (site,function),
	CONSTRAINT		fk_execution_service_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

CREATE TABLE glidein (
	id				BIGINT NOT NULL AUTO_INCREMENT,
	site			BIGINT NOT NULL,
	count			INTEGER,
	hostCount		INTEGER,
	wallTime		INTEGER,
	numCpus			INTEGER,
	configBase64	TEXT,
	gcbBroker		VARCHAR(15),
	idleTime		INTEGER,
	debug			TEXT,
	status			VARCHAR(16),
	statusMessage	VARCHAR(1024),
	CONSTRAINT		pk_glidein PRIMARY KEY (id),
	CONSTRAINT		fk_glidein_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

CREATE TABLE glidein_environment (
	glidein			BIGINT NOT NULL,
	variable		VARCHAR(255) NOT NULL,
	value			TEXT,
	CONSTRAINT		pk_glidein_environment PRIMARY KEY (glidein,variable),
	CONSTRAINT		fk_glidein_environment_01 FOREIGN KEY (glidein) REFERENCES glidein(id) ON DELETE CASCADE
) type=InnoDB;