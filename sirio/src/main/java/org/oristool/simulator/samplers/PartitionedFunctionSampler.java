package org.oristool.simulator.samplers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.function.EXP;
import org.oristool.math.function.Erlang;
import org.oristool.math.function.Function;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.util.Pair;

public final class PartitionedFunctionSampler implements Sampler {
	
	private final PartitionedFunction partitionedFunction;
	List<Pair<BigDecimal,Sampler>> samplers = new ArrayList<Pair<BigDecimal,Sampler>>();

	public PartitionedFunctionSampler(PartitionedFunction partitionedFunction) {
		
		this.partitionedFunction = partitionedFunction;
	
		for (Function f : this.partitionedFunction.getFunctions()) {
			
			if (f.getDensities().size() == 1) {
				
				if (f instanceof GEN) { 

					GEN gen = (GEN)f;
					OmegaBigDecimal eft = gen.getDomainsEFT();
					OmegaBigDecimal lft = gen.getDomainsLFT();
					BigDecimal probability =  gen.integrateOverDomain().bigDecimalValue();

					if (gen.getDensity().isConstant()) {
						Sampler sampler = new UniformSampler(eft.bigDecimalValue(), lft.bigDecimalValue());
						samplers.add(Pair.of(probability,sampler));
					}

					else if (gen.getDensity().isExponential()) {
						Sampler sampler = new ShiftedExponentialSampler(gen.getDensity().getExponentialRate(), eft.bigDecimalValue());
						samplers.add(Pair.of(probability,sampler));
					}

					else {
						Sampler sampler = new MetropolisHastings(gen);
						samplers.add(Pair.of(probability,sampler));
					}					
				} 
				
				else if (f instanceof EXP) {
					new IllegalArgumentException("A function of a partitioned function cannot be an EXP");		
				}
				
				else if (f instanceof Erlang) {
					new IllegalArgumentException("A function of a partitioned function cannot be an Erlang");					
				}

				else new IllegalArgumentException("Function " + f.getClass().toString() + " is not supported");					

			} else {
				new IllegalArgumentException("Nesting of partitioned functions is not allowed");
			}						
		}
	}
	
	@Override
	public BigDecimal getSample() {
		BigDecimal rand = new BigDecimal(Math.random());
		BigDecimal sum = BigDecimal.ZERO;
		for (Pair<BigDecimal,Sampler> pair : samplers) {
			sum = sum.add(pair.first());
			if (rand.compareTo(sum)<=0) {
				return pair.second().getSample();
			}
		}
		throw new IllegalArgumentException("The integral of a partitioned function over its domain must be 1");
	}

}
