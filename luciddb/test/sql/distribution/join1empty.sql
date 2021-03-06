--
-- testing join with one side empty
-- (filter out by the "AND" condition in the queries)
--
set schema 's';

select b1.k100, b1.kseq, b2.kseq, b2.k100
from distribution_100 b1, distribution_100 b2
where b1.k100square = b2.kseqsquare
  and b1.k100square > 18
--order by b1.k100, b1.kseq, b2.kseq, b2.k100;
order by 1,2,3,4;

select b1.k10k, b1.kseq, b2.kseq, b2.k10k
from distribution_10k b1, distribution_10k b2
where b1.k10ksquare = b2.kseqsquare
  and b1.k10ksquare > 18
--order by b1.k10k, b1.kseq, b2.kseq, b2.k10k;
order by 1,2,3,4;

select b1.k100k, b1.kseq, b2.kseq, b2.k100k
from distribution_100k b1, distribution_100k b2
where b1.k100ksquare = b2.kseqsquare
  and b1.k100ksquare > 18
--order by b1.k100k, b1.kseq, b2.kseq, b2.k100k;
order by 1,2,3,4;

select b1.k500k, b1.kseq, b2.kseq, b2.k500k
from distribution_1m b2, distribution_1m b1
where b1.k500ksquare = b2.kseqsquare
  and b1.k500ksquare > 18
--order by b1.k500k, b1.kseq, b2.kseq, b2.k500k;
order by 1,2,3,4;
