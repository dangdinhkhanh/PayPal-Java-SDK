--
-- PostgreSQL database dump
--

-- Dumped from database version 10.4
-- Dumped by pg_dump version 10.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: invoice; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invoice (
    invoiceid integer NOT NULL,
    renteename character varying(255) NOT NULL,
    paymentmethod character varying(20),
    status character varying(20),
    amount integer NOT NULL,
    currency character varying(3),
    createddate timestamp without time zone,
    updateddate timestamp without time zone
);


ALTER TABLE public.invoice OWNER TO postgres;

--
-- Name: payment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payment (
    paymentid integer NOT NULL,
    invoiceid integer NOT NULL,
    paymentmethod character varying(20),
    amount integer NOT NULL,
    currency character varying(3),
    status character varying(20),
    externalid character varying(50),
    createddate timestamp without time zone,
    updateddate timestamp without time zone,
    transaction_id character varying(20)
);


ALTER TABLE public.payment OWNER TO postgres;

--
-- Name: payment_paymentid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.payment_paymentid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_paymentid_seq OWNER TO postgres;

--
-- Name: payment_paymentid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.payment_paymentid_seq OWNED BY public.payment.paymentid;


--
-- Name: payment paymentid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment ALTER COLUMN paymentid SET DEFAULT nextval('public.payment_paymentid_seq'::regclass);


--
-- Data for Name: invoice; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.invoice (invoiceid, renteename, paymentmethod, status, amount, currency, createddate, updateddate) FROM stdin;
2000	dang dinh khanh	paypal	no pay	2000000	VND	2018-07-03 10:00:00	\N
3000	dinh thi ty mai	cash	no pay	1000	USD	2018-07-03 09:00:00	\N
1000	le chi cuong	paypal	approved	1000	EUR	2018-07-04 10:00:00	\N
10000	dang tuong vi	paypal	approved	10	EUR	2018-07-14 10:00:00	2018-07-19 22:08:51
5000	dinh thi nhai	paypal	approved	10	EUR	2018-07-15 10:00:00	2018-07-19 23:03:48
50000	dinh van viet	paypal	approved	100	EUR	2018-07-15 10:00:00	2018-08-28 16:10:19
500000	dang dinh kha	paypal	completed	10	EUR	2018-08-29 10:00:00	2018-08-29 01:18:12
600000	pham thi tran hang	paypal	fully paid	10	EUR	2018-08-29 10:00:00	2018-08-29 14:46:12
\.


--
-- Data for Name: payment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payment (paymentid, invoiceid, paymentmethod, amount, currency, status, externalid, createddate, updateddate, transaction_id) FROM stdin;
1	5000	paypal	10	EUR	created	PAY-6V1207681S123481PLNIPY7A	2018-07-19 23:02:52	\N	\N
2	50000	paypal	100	EUR	approved	PAY-41R00358SH920393XLOCVO7A	2018-08-28 16:08:57	2018-08-28 16:10:19	\N
3	500000	paypal	10	EUR	approved	PAY-19X13385N4491333RLOC5PTQ	2018-08-29 01:16:27	2018-08-29 01:18:12	\N
4	600000	paypal	10	EUR	created	PAY-6UC52075GJ037802BLODJKYQ	2018-08-29 14:45:18	\N	\N
5	600000	paypal	10	EUR	created	PAY-6EN549278H033673ALODJKYY	2018-08-29 14:45:19	\N	\N
\.


--
-- Name: payment_paymentid_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.payment_paymentid_seq', 5, true);


--
-- Name: invoice invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (invoiceid);


--
-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (paymentid);


--
-- Name: payment payment_invoiceid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_invoiceid_fkey FOREIGN KEY (invoiceid) REFERENCES public.invoice(invoiceid);


--
-- PostgreSQL database dump complete
--

