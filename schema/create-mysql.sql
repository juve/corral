CREATE TABLE site ( 
	id				BIGINT NOT NULL AUTO_INCREMENT,
	name			VARCHAR(255) NOT NULL,
	installPath 	TEXT NOT NULL,
	localPath		TEXT NOT NULL,
	submitPath		TEXT NOT NULL,
	condorPackage	VARCHAR(1024),
	condorVersion	VARCHAR(16),
	status			VARCHAR(16) NOT NULL,
	statusMessage	VARCHAR(1024) NOT NULL,
	submitted		DATETIME NOT NULL,
	lastUpdate		DATETIME NOT NULL,
	CONSTRAINT pk_site PRIMARY KEY (id)
) type=InnoDB;

CREATE TABLE environment (
	site			BIGINT NOT NULL,
	variable		VARCHAR(255) NOT NULL,
	value			TEXT,
	CONSTRAINT pk_environment PRIMARY KEY (site,variable),
	CONSTRAINT fk_environment_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

CREATE TABLE execution_service (
	site			BIGINT NOT NULL,
	function		ENUM('STAGING','GLIDEIN') NOT NULL,
	serviceContact	VARCHAR(1024) NOT NULL,
	serviceType		ENUM('GT2','GT4') NOT NULL,
	project			VARCHAR(255),
	queue			VARCHAR(255),
	CONSTRAINT pk_execution_service PRIMARY KEY (site,function),
	CONSTRAINT fk_execution_service_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

CREATE TABLE glidein (
	id					BIGINT NOT NULL AUTO_INCREMENT,
	site				BIGINT NOT NULL,
	condorHost			VARCHAR(256) NOT NULL,
	count				INTEGER NOT NULL,
	hostCount			INTEGER NOT NULL,
	wallTime			INTEGER NOT NULL,
	numCpus				INTEGER NOT NULL,
	condorConfigBase64	TEXT,
	gcbBroker			VARCHAR(15),
	idleTime			INTEGER,
	condorDebug			TEXT,
	status				VARCHAR(16) NOT NULL,
	statusMessage		VARCHAR(1024) NOT NULL,
	submitted			DATETIME NOT NULL,
	lastUpdate			DATETIME NOT NULL,
	CONSTRAINT pk_glidein PRIMARY KEY (id),
	CONSTRAINT fk_glidein_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;