package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.basedata.entity.ProductPurchase;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.mappers.ProductPurchaseMapper;
import com.lframework.xingyun.basedata.service.product.ProductPurchaseService;
import com.lframework.xingyun.basedata.vo.product.purchase.CreateProductPurchaseVo;
import com.lframework.xingyun.basedata.vo.product.purchase.UpdateProductPurchaseVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductPurchaseServiceImpl extends
		BaseMpServiceImpl<ProductPurchaseMapper, ProductPurchase>
		implements ProductPurchaseService {

	@OpLog(type = BaseDataOpLogType.class, name = "设置商品采购价，ID：{}, 采购价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public String create(CreateProductPurchaseVo vo) {

		ProductPurchase data = getProductPurchase(vo);

		getBaseMapper().insert(data);

		return data.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchCreate(List<CreateProductPurchaseVo> vos) {
		if (vos == null || vos.isEmpty()) {
			return;
		}
		List<ProductPurchase> productPurchases = vos.stream()
				.map(ProductPurchaseServiceImpl::getProductPurchase)
				.collect(Collectors.toList());
		saveBatch(productPurchases);
	}

	private static ProductPurchase getProductPurchase(CreateProductPurchaseVo vo) {
		ProductPurchase data = new ProductPurchase();
		data.setId(IdUtil.getId());
		if (!StringUtil.isBlank(vo.getId())) {
			data.setId(vo.getId());
		}

		data.setPrice(vo.getPrice());
		return data;
	}


	@OpLog(type = BaseDataOpLogType.class, name = "设置商品采购价，ID：{}, 采购价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void update(UpdateProductPurchaseVo vo) {

		if (vo.getPrice() == null) {
			throw new InputErrorException("采购价不能为空！");
		}

		if (vo.getPrice().doubleValue() < 0D) {
			throw new InputErrorException("采购价必须大于0！");
		}

		getBaseMapper().deleteById(vo.getId());

		CreateProductPurchaseVo createVo = new CreateProductPurchaseVo();
		createVo.setId(vo.getId());
		createVo.setPrice(vo.getPrice());

		this.create(createVo);
	}
}
