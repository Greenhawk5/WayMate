import { ApiFieldError } from '../types.js';

export function validateCoordinates(lat: unknown, lon: unknown): ApiFieldError[] {
  const errors: ApiFieldError[] = [];
  if (typeof lat !== 'number' || isNaN(lat) || lat < -90.0 || lat > 90.0) {
    errors.push({
      field: 'latitude',
      issue: 'عرض جغرافیایی باید یک عدد معتبر بین ۹۰- تا ۹۰+ باشد',
    });
  }
  if (typeof lon !== 'number' || isNaN(lon) || lon < -180.0 || lon > 180.0) {
    errors.push({
      field: 'longitude',
      issue: 'طول جغرافیایی باید یک عدد معتبر بین ۱۸۰- تا ۱۸۰+ باشد',
    });
  }
  return errors;
}

export function validateTelemetryPayload(data: any): ApiFieldError[] {
  const errors: ApiFieldError[] = [];

  if (!data || typeof data !== 'object') {
    errors.push({ field: 'body', issue: 'بدنه درخواست باید یک شیء JSON معتبر باشد' });
    return errors;
  }

  errors.push(...validateCoordinates(data.latitude, data.longitude));

  if (typeof data.accuracy_meters !== 'number' || data.accuracy_meters < 0) {
    errors.push({
      field: 'accuracy_meters',
      issue: 'دقت موقعیت باید عددی مثبت باشد',
    });
  }

  if (data.speed_mps !== undefined && (typeof data.speed_mps !== 'number' || data.speed_mps < 0)) {
    errors.push({
      field: 'speed_mps',
      issue: 'سرعت حرکت نمی‌تواند منفی باشد',
    });
  }

  if (
    data.heading_degrees !== undefined &&
    (typeof data.heading_degrees !== 'number' || data.heading_degrees < 0 || data.heading_degrees > 360)
  ) {
    errors.push({
      field: 'heading_degrees',
      issue: 'زاویه حرکت (سمت‌نما) باید بین ۰ تا ۳۶۰ درجه باشد',
    });
  }

  if (
    data.operational_status !== undefined &&
    !['NORMAL', 'PURSUING', 'ENGAGED'].includes(data.operational_status)
  ) {
    errors.push({
      field: 'operational_status',
      issue: 'وضعیت عملیاتی باید یکی از مقادیر NORMAL، PURSUING یا ENGAGED باشد',
    });
  }

  if (typeof data.recorded_at === 'number') {
    const now = Date.now();
    const driftSeconds = Math.abs(now - data.recorded_at) / 1000;
    if (driftSeconds > 3600) {
      errors.push({
        field: 'recorded_at',
        issue: 'زمان ثبت موقعیت با زمان سرور بیش از ۱ ساعت اختلاف دارد',
      });
    }
  }

  return errors;
}

export function validateIncidentPayload(data: any): ApiFieldError[] {
  const errors: ApiFieldError[] = [];

  if (!data || typeof data !== 'object') {
    errors.push({ field: 'body', issue: 'بدنه درخواست نامعتبر است' });
    return errors;
  }

  if (typeof data.title !== 'string' || data.title.trim().length === 0 || data.title.length > 120) {
    errors.push({
      field: 'title',
      issue: 'عنوان رخداد باید متنی بین ۱ تا ۱۲۰ کاراکتر باشد',
    });
  }

  if (typeof data.description !== 'string' || data.description.trim().length === 0 || data.description.length > 2000) {
    errors.push({
      field: 'description',
      issue: 'شرح رخداد الزامی بوده و حداکثر ۲۰۰۰ کاراکتر است',
    });
  }

  const validTypes = ['SUSPICIOUS_PERSON', 'VEHICLE_TRESPASS', 'ROAD_HAZARD', 'SECURITY_BREACH', 'QUICK_ALERT', 'OTHER'];
  if (typeof data.incident_type !== 'string' || !validTypes.includes(data.incident_type)) {
    errors.push({
      field: 'incident_type',
      issue: `نوع رخداد باید یکی از موارد زیر باشد: ${validTypes.join(', ')}`,
    });
  }

  const validSeverities = ['ROUTINE', 'WARNING', 'URGENT'];
  if (typeof data.severity !== 'string' || !validSeverities.includes(data.severity)) {
    errors.push({
      field: 'severity',
      issue: `سطح فوریت باید یکی از موارد زیر باشد: ${validSeverities.join(', ')}`,
    });
  }

  errors.push(...validateCoordinates(data.latitude, data.longitude));

  return errors;
}
