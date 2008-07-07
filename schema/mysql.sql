/* Delete any existing tables */
drop table if exists interface;
drop table if exists execution_service;
drop table if exists glidein_history;
drop table if exists glidein;
drop table if exists environment;
drop table if exists site_history;
drop table if exists site;

/**
 * Table to keep track of the version of this database. It contains the version
 * of the database and the date that it was created.
 */
CREATE TABLE interface (
	revision	VARCHAR(25) NOT NULL,	-- SVN revision number of database creation script
	created		DATETIME NOT NULL		-- Date/time when tables were created
) type=InnoDB;

INSERT INTO interface (revision,created) VALUES ('$Revision$',NOW());

/**
 * Table to store information about sites. This includes any data needed
 * to help install the condor executables at the site and any current status
 * information.
 */
CREATE TABLE site ( 
	id				BIGINT NOT NULL AUTO_INCREMENT,		-- Site ID number
	name			VARCHAR(255) NOT NULL,				-- The name of the site
	installPath 	TEXT NOT NULL,						-- Path where executables are kept
	localPath		TEXT NOT NULL,						-- Path where local files are kept (logs, etc.)
	condorPackage	VARCHAR(1024),						-- Name of the condor package (e.g. 7.0.0-x86_64-Linux-glibc2.3)
	condorVersion	VARCHAR(16),						-- Version of condor to install (e.g. 7.0.0)
	state			VARCHAR(16) NOT NULL,				-- The current state (e.g. READY)
	shortMessage	VARCHAR(256) NOT NULL,				-- A short status message
	longMessage		TEXT,								-- A long status message (e.g. Stack trace)
	created			DATETIME NOT NULL,					-- Time when site was created
	lastUpdate		DATETIME NOT NULL,					-- Time when site was last updated
	CONSTRAINT pk_site PRIMARY KEY (id)
) type=InnoDB;

/**
 * Each site has a list of built-in environment variables that will be
 * included in each job submitted for that site.
 */
CREATE TABLE environment (
	site			BIGINT NOT NULL,					-- Site ID
	variable		VARCHAR(255) NOT NULL,				-- The name of the variable (e.g. PATH)
	value			TEXT,								-- The value of the variable
	CONSTRAINT pk_environment PRIMARY KEY (site,variable),
	CONSTRAINT fk_environment_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

/**
 * Each site has two kinds of execution services: STAGING and GLIDEIN that
 * specify how jobs are submitted to the site. Staging is used for site setup
 * jobs, and glidein is used for glidein jobs. Right now only GT2 and GT4 are
 * supported, but in the future any Condor job type could be supported.
 */
CREATE TABLE execution_service (
	site			BIGINT NOT NULL,					-- Site ID
	function		ENUM('STAGING','GLIDEIN') NOT NULL,	-- Use for staging or glideins
	serviceContact	VARCHAR(1024) NOT NULL,				-- The URL of the grid service
	serviceType		ENUM('GT2','GT4') NOT NULL,			-- The version of grid service
	project			VARCHAR(255),						-- The desired project name/id
	queue			VARCHAR(255),						-- The target queue name/id
	CONSTRAINT pk_execution_service PRIMARY KEY (site,function),
	CONSTRAINT fk_execution_service_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE CASCADE
) type=InnoDB;

/**
 * Table to keep track of glideins. Each site has zero or more running
 * glideins.
 */
CREATE TABLE glidein (
	id					BIGINT NOT NULL AUTO_INCREMENT,	-- Glidein ID
	site				BIGINT,							-- Site ID
	condorHost			VARCHAR(256) NOT NULL,			-- Host where condor CM runs
	count				INTEGER NOT NULL,				-- Total number of processes
	hostCount			INTEGER NOT NULL,				-- Total number of hosts
	wallTime			INTEGER NOT NULL,				-- Runtime in minutes
	numCpus				INTEGER NOT NULL,				-- Num CPUs / process
	condorConfig		BLOB,							-- Custom config file (base 64 encoded)
	gcbBroker			VARCHAR(15),					-- The GCB broker IP address
	idleTime			INTEGER,						-- Idle time in minutes
	condorDebug			TEXT,							-- Debugging flags for condor daemons
	state				VARCHAR(16) NOT NULL,			-- The current state of the glidein (e.g. RUNNING)
	shortMessage		VARCHAR(256) NOT NULL,			-- Short status message
	longMessage			TEXT,							-- Long status message (e.g. stack trace)
	created				DATETIME NOT NULL,				-- Date when glidein was created
	lastUpdate			DATETIME NOT NULL,				-- Date when glidein was last updated
	resubmit			BOOLEAN NOT NULL,				-- Should the glidein be resubmitted when it terminates?
	resubmits			INTEGER,						-- Number of times to resubmit
	until				DATETIME,						-- Date to resubmit until
	submits				INTEGER NOT NULL,				-- Total number of times this glidein has be submitted
	rsl					TEXT,							-- Globus RSL or XML to override other parameters
	CONSTRAINT pk_glidein PRIMARY KEY (id),
	CONSTRAINT fk_glidein_01 FOREIGN KEY (site) REFERENCES site(id) ON DELETE SET NULL
) type=InnoDB;

/**
 * History of glidein state changes
 */
CREATE TABLE glidein_history (
	glidein				BIGINT NOT NULL,				-- Glidein ID
	state				VARCHAR(16) NOT NULL,			-- New state
	time				DATETIME NOT NULL				-- Date/time when glidein entered state
	/* No foreign key constraint, history is not deleted */
) type=InnoDB;

/**
 * History of site state changes
 */
CREATE TABLE site_history (
	site				BIGINT NOT NULL,				-- Site id
	state				VARCHAR(16) NOT NULL,			-- New state
	time				DATETIME NOT NULL				-- Date/time when site entered state
	/* No foreign key constraint, history is not deleted */
) type=InnoDB;