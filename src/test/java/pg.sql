DROP TABLE IF EXISTS public.slim_test_entity;
CREATE TABLE public.slim_test_entity
(
  name character varying not null,
  id serial NOT NULL,
  count integer,
  CONSTRAINT slim_test_entity_pkey PRIMARY KEY (id)
);
ALTER TABLE public.slim_test_entity
  OWNER TO slimuser;
