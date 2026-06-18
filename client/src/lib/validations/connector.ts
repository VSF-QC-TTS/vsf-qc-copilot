import { z } from 'zod';

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

export const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;
export const AUTH_TYPES = ['NONE', 'BEARER', 'API_KEY', 'BASIC'] as const;
export const BODY_TYPES = ['NONE', 'RAW_JSON', 'RAW_TEXT'] as const;
export const RESPONSE_FORMATS = ['JSON', 'TEXT', 'SSE'] as const;

// ---------------------------------------------------------------------------
// Create / Edit Connector
// ---------------------------------------------------------------------------

export const createConnectorSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),
  protocol: z.string().optional().or(z.literal('')),
  method: z.enum(HTTP_METHODS, { message: 'HTTP method is required' }),
  baseUrl: z.url('Must be a valid URL'),
  path: z.string().optional().or(z.literal('')),
  bodyType: z.enum(BODY_TYPES).optional(),
  bodyTemplate: z.string().optional().or(z.literal('')),
  bodyTemplateText: z.string().optional().or(z.literal('')),
  responseFormat: z.enum(RESPONSE_FORMATS).optional(),
  responseSelector: z.string().trim().min(1, 'required'),
  authType: z.enum(AUTH_TYPES).optional(),
  timeoutSeconds: z.coerce
    .number()
    .int()
    .min(1, 'required')
    .max(300)
    .default(30),
  retryCount: z.coerce
    .number()
    .int()
    .min(0, 'Minimum 0')
    .max(5)
    .default(0),
  active: z.boolean().default(true),
  isStreaming: z.boolean().default(false),
});

export type CreateConnectorFormValues = z.input<typeof createConnectorSchema>;

export const createConnectorFromCurlSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  rawCurl: z.string().min(1, 'required'),
  description: z.string().max(1000).optional().or(z.literal('')),
  responseSelector: z.string().trim().optional().or(z.literal('')),
  timeoutSeconds: z.coerce
    .number()
    .int()
    .min(1, 'required')
    .max(300)
    .default(60),
  retryCount: z.coerce
    .number()
    .int()
    .min(0, 'Minimum 0')
    .max(5)
    .default(1),
});

export type CreateConnectorFromCurlFormValues = z.input<typeof createConnectorFromCurlSchema>;

// ---------------------------------------------------------------------------
// Test Run
// ---------------------------------------------------------------------------

export const testRunSchema = z.object({
  question: z.string().min(1, 'required'),
  precondition: z.string().optional().or(z.literal('')),
  metadata: z.string().optional().or(z.literal('')),
});

export type TestRunFormValues = z.infer<typeof testRunSchema>;
