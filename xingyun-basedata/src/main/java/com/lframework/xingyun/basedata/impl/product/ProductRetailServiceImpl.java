package com.lframework.xingyun.basedata.impl.product;

import cn.hutool.core.collection.CollectionUtil;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.basedata.entity.ProductRetail;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.mappers.ProductRetailMapper;
import com.lframework.xingyun.basedata.service.product.ProductRetailService;
import com.lframework.xingyun.basedata.vo.product.retail.CreateProductRetailVo;
import com.lframework.xingyun.basedata.vo.product.retail.UpdateProductRetailVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductRetailServiceImpl extends BaseMpServiceImpl<ProductRetailMapper, ProductRetail>
		implements ProductRetailService {

	@OpLog(type = BaseDataOpLogType.class, name = "设置商品零售价，ID：{}, 零售价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public String create(CreateProductRetailVo vo) {

		ProductRetail data = getProductRetail(vo);

		getBaseMapper().insert(data);

		return data.getId();
	}

	@OpLog(type = BaseDataOpLogType.class, name = "设置商品零售价，ID：{}, 零售价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void update(UpdateProductRetailVo vo) {

		if (vo.getPrice() == null) {
			throw new InputErrorException("零售价不能为空！");
		}

		if (vo.getPrice().doubleValue() < 0D) {
			throw new InputErrorException("零售价必须大于0！");
		}

		getBaseMapper().deleteById(vo.getId());

		CreateProductRetailVo createVo = new CreateProductRetailVo();
		createVo.setId(vo.getId());
		createVo.setPrice(vo.getPrice());

		this.create(createVo);
	}

	@Override
	public void batchCreate(List<CreateProductRetailVo> createProductRetailVoList) {
		if (CollectionUtil.isEmpty(createProductRetailVoList)) {
			return;
		}

		List<ProductRetail> collect = createProductRetailVoList.stream()
				.map(ProductRetailServiceImpl::getProductRetail)
				.collect(Collectors.toList());

		saveBatch(collect);
	}

	private static ProductRetail getProductRetail(CreateProductRetailVo item) {
		ProductRetail data = new ProductRetail();
		data.setId(IdUtil.getId());
		if (!StringUtil.isBlank(item.getId())) {
			data.setId(item.getId());
		}

		data.setPrice(item.getPrice());
		return data;
	}
}
